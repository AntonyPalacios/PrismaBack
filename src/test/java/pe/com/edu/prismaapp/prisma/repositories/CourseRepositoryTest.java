package pe.com.edu.prismaapp.prisma.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;
import org.springframework.test.context.jdbc.Sql;
import pe.com.edu.prismaapp.prisma.config.ContainersConfig;


import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ImportTestcontainers(ContainersConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)

class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @Sql(scripts = "/data-courses.sql")
    public void repositoryShouldContainOnly2Sections() {
        //Act
        var sections = courseRepository.findByParentCourseIsNull();
        //Assert
        assertEquals(2, sections.size());
    }

    @Test
    @Sql(scripts = "/data-courses.sql")
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
    @Sql(scripts = "/data-courses.sql")
    public void LectCourseShouldHaveId1() {
        //Act
        var sections = courseRepository.findByParentCourseIsNull();
        var course = sections.stream().filter(section -> section.getAbbreviation().equals("LECT")).findFirst();

        //Assert
        assertEquals(1L, course.get().getId());
    }

    @Test
    @Sql(scripts = "/data-courses.sql")
    public void MateCourseShouldHaveId2() {
        //Act
        var sections = courseRepository.findByParentCourseIsNull();
        var course = sections.stream().filter(section -> section.getAbbreviation().equals("MATE")).findFirst();

        //Assert
        assertEquals(2L, course.get().getId());
    }

    @Test
    @Sql(scripts = "/data-courses.sql")
    public void MateCourseShouldHave5Courses() {
        //Act
        var sections = courseRepository.findByParentCourseIsNotNull()
                .stream().filter(section -> section.getParentCourse().getId() == 2L).toList();

        //Assert
        assertEquals(5, sections.size());
    }

    @Test
    @Sql(scripts = "/data-courses.sql")
    public void LectCourseShouldHave1Course() {
        //Act
        var sections = courseRepository.findByParentCourseIsNotNull().stream().filter(section -> section.getParentCourse().getId() == 1L).toList();

        //Assert
        assertEquals(1, sections.size());
    }

}