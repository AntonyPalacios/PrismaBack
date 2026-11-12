package pe.com.edu.prisma.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import pe.com.edu.prisma.config.ContainersConfig;
import pe.com.edu.prisma.security.JwtService;
import pe.com.edu.prisma.users.User;
import pe.com.edu.prisma.users.UserRepository;
import pe.com.edu.prisma.users.UserService;

/**
 * The logging agreement (4.2 "Logs") end to end: the errorId of an error
 * response is the traceId printed on that request's log lines, an error is
 * logged exactly once with the endpoint and its sanitized arguments, and
 * personal data never reaches the log in clear text.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Import(ContainersConfig.class)
@ExtendWith(OutputCaptureExtension.class)
class LoggingIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @MockitoSpyBean
    private UserService userService;

    private ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(user("admin").roles("ADMIN")).with(csrf())
                .contentType(MediaType.APPLICATION_JSON));
    }

    private static String errorIdOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.errorId");
    }

    private static List<String> linesContaining(CapturedOutput output, String text) {
        return output.getOut().lines().filter(line -> line.contains(text)).toList();
    }

    @Test
    void anUnexpectedErrorIsLoggedOnceWithTraceEndpointAndMaskedArguments(CapturedOutput output) throws Exception {
        doThrow(new IllegalStateException("boom")).when(userService).create(any());
        String email = "user." + UUID.randomUUID().toString().substring(0, 8) + "@prisma.edu.pe";
        String body = "{\"email\":\"" + email + "\",\"firstName\":\"Antony\",\"lastNamePat\":\"Palacios\","
                + "\"lastNameMat\":null,\"roles\":[\"TUTOR\"]}";

        String errorId = errorIdOf(asAdmin(post("/api/v1/users").content(body))
                .andExpect(status().isInternalServerError()));

        List<String> errorLines = linesContaining(output, "500 on");
        assertThat(errorLines).hasSize(1);
        assertThat(errorLines.get(0))
                .contains("ERROR")
                .contains("trace=" + errorId)
                .contains("POST /api/v1/users")
                .contains("UserController#create")
                .contains("email=u***@prisma.edu.pe", "firstName=Ant***", "lastNamePat=Pal***", "roles=[size=1]");
        // the stack trace is there, and it is the only time the exception shows up
        assertThat(output.getOut()).containsOnlyOnce("java.lang.IllegalStateException: boom");
        assertThat(output.getOut()).doesNotContain(email, "Antony", "Palacios");
    }

    @Test
    void aNotFoundIsLoggedAtInfoWithTheSameTraceIdTheUserGets(CapturedOutput output) throws Exception {
        String errorId = errorIdOf(asAdmin(get("/api/v1/users/987654321")).andExpect(status().isNotFound()));

        List<String> lines = linesContaining(output, "404 on");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0))
                .contains("INFO")
                .contains("trace=" + errorId)
                .contains("UserController#findById args={id=987654321}");
    }

    @Test
    void aBrokenBusinessRuleIsLoggedAtWarn(CapturedOutput output) throws Exception {
        long permanentAdminId = userRepository.findByEmailIgnoreCase(User.PERMANENT_ADMIN_EMAIL).orElseThrow().getId();

        String errorId = errorIdOf(asAdmin(delete("/api/v1/users/" + permanentAdminId))
                .andExpect(status().isConflict()));

        List<String> lines = linesContaining(output, "409 on");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).contains("WARN").contains("trace=" + errorId)
                .contains("UserController#deactivate args={id=" + permanentAdminId + "}");
    }

    @Test
    void aValidationErrorNeverLogsTheRejectedValues(CapturedOutput output) throws Exception {
        String body = "{\"email\":\"secret-not-an-email\",\"firstName\":\"\",\"lastNamePat\":\"Palacios\","
                + "\"lastNameMat\":null,\"roles\":[\"TUTOR\"]}";

        asAdmin(post("/api/v1/users").content(body)).andExpect(status().isBadRequest());

        List<String> lines = linesContaining(output, "400 on");
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).contains("INFO").contains("invalid fields").contains("email", "firstName");
        assertThat(output.getOut()).doesNotContain("secret-not-an-email");
    }

    @Test
    void aRealSessionPutsTheUserIdOnTheLogLine(CapturedOutput output) throws Exception {
        User admin = userRepository.findByEmailIgnoreCase(User.PERMANENT_ADMIN_EMAIL).orElseThrow();
        Cookie session = new Cookie("prisma_token", jwtService.issueToken(admin.getId(), admin.getEmail()));

        mockMvc.perform(get("/api/v1/users/987654321").cookie(session)).andExpect(status().isNotFound());

        assertThat(linesContaining(output, "404 on")).singleElement().asString()
                .contains("user=" + admin.getId() + "]");
    }

    @Test
    void logsAlsoGoToTheLogFile() throws Exception {
        asAdmin(get("/api/v1/users/987654321")).andExpect(status().isNotFound());

        Path logFile = Path.of("target/logs/prisma-test.log");
        assertThat(logFile).exists();
        assertThat(Files.readString(logFile)).contains("404 on GET /api/v1/users/987654321");
    }
}
