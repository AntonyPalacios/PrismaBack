package pe.com.edu.prismaapp.prisma.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.jdbc.Sql;
import pe.com.edu.prismaapp.prisma.config.ContainersConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
@ImportTestcontainers(ContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Sql(scripts = "/data-courses.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    public void repositoryShouldContainOnly2Sections() {
        //Act
        var sections = courseRepository.findByParentCourseIsNull();
        //Assert
        assertEquals(2, sections.size());
    }

    @Test
    public void repositoryShouldOnlyHave1LECTand1MATE() {
        //Act
        var sections = courseRepository.findByParentCourseIsNull();

        var sizeLect = sections.stream().filter(section -> section.getAbbreviation().equals("LECT")).count();
        var sizeMate = sections.stream().filter(section -> section.getAbbreviation().equals("MATE")).count();

        //Assert
        assertEquals(1, sizeLect);
        assertEquals(1, sizeMate);
    }

    @Test
    public void LectCourseShouldHaveId1() {
        //Act
        var sections = courseRepository.findByParentCourseIsNull();
        var course = sections.stream().filter(section -> section.getAbbreviation().equals("LECT")).findFirst();

        //Assert
        assertEquals(1L, course.get().getId());
    }

    @Test
    public void MateCourseShouldHaveId2() {
        //Act
        var sections = courseRepository.findByParentCourseIsNull();
        var course = sections.stream().filter(section -> section.getAbbreviation().equals("MATE")).findFirst();

        //Assert
        assertEquals(2L, course.get().getId());
    }

    @Test
    public void MateCourseShouldHave5Courses() {
        //Act
        var sections = courseRepository.findByParentCourseIsNotNull()
                .stream().filter(section -> section.getParentCourse().getId() == 2L).toList();

        //Assert
        assertEquals(5, sections.size());
    }

    @Test
    public void LectCourseShouldHave1Course() {
        //Act
        var sections = courseRepository.findByParentCourseIsNotNull().stream().filter(section -> section.getParentCourse().getId() == 1L).toList();

        //Assert
        assertEquals(1, sections.size());
    }

    @Test
    public void LectSectionShouldHave1CourseWithNameLECT() {
        //Act
        var lectCourse = courseRepository.findByAbbreviationAndParentCourseIsNotNull("LECT");

        //Assert
        assertNotNull(lectCourse);
        assertEquals(1L, lectCourse.getParentCourse().getId());
    }

    @Test
    public void MateSectionShouldHave5Courses() {
        //Act
        var nyoCourse = courseRepository.findByAbbreviationAndParentCourseIsNotNull("NYO");
        var geoCourse = courseRepository.findByAbbreviationAndParentCourseIsNotNull("GEO");
        var xCourse = courseRepository.findByAbbreviationAndParentCourseIsNotNull("X");
        var trigoCourse = courseRepository.findByAbbreviationAndParentCourseIsNotNull("TRIGO");
        var estCourse = courseRepository.findByAbbreviationAndParentCourseIsNotNull("EST");

        //Assert
        assertNotNull(nyoCourse);
        assertEquals(2L, nyoCourse.getParentCourse().getId());

        assertNotNull(geoCourse);
        assertEquals(2L, geoCourse.getParentCourse().getId());

        assertNotNull(xCourse);
        assertEquals(2L, xCourse.getParentCourse().getId());

        assertNotNull(trigoCourse);
        assertEquals(2L, trigoCourse.getParentCourse().getId());

        assertNotNull(estCourse);
        assertEquals(2L, estCourse.getParentCourse().getId());
    }

}