package pe.com.edu.prismaapp.prisma.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.com.edu.prismaapp.prisma.util.ExamColumnsUtil;

@Configuration
public class ExamColConfig {
    @Bean
    public ExamColumnsUtil.CourseColumns courseColumns() {
        return new ExamColumnsUtil.CourseColumns(
                7,8,9,
                14,15,16,
                17,18,19,
                20,21,22,
                23,24,25,
                26,27,28);
    }

    @Bean("arqui")
    public ExamColumnsUtil.AreaConfig arquiExamColumns() {
        return new ExamColumnsUtil.AreaConfig(28, 29, 30, 32);
    }

    @Bean("ciencias")
    public ExamColumnsUtil.AreaConfig cienciasExamColumns() {
        return new ExamColumnsUtil.AreaConfig(31, 32, 33, 35);
    }

    @Bean("letras")
    public ExamColumnsUtil.AreaConfig letrasExamColumns() {
        return new ExamColumnsUtil.AreaConfig(34, 35, 36, 38);
    }
}
