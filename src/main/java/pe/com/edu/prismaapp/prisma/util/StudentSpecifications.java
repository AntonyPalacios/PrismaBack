package pe.com.edu.prismaapp.prisma.util;

import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import pe.com.edu.prismaapp.prisma.entities.Student;

import java.util.ArrayList;
import java.util.List;

public class StudentSpecifications {

    public static Specification<Student> searchWithTypos(String[] words) {
        return (root, query, criteriaBuilder) -> {

            // 1. El nombre completo concatenado (para búsquedas parciales exactas)
            Expression<String> fullName = criteriaBuilder.concat(
                    criteriaBuilder.concat(root.get("name"), criteriaBuilder.literal(" ")),
                    criteriaBuilder.concat(
                            criteriaBuilder.concat(root.get("apat"), criteriaBuilder.literal(" ")),
                            root.get("amat")
                    )
            );
            Expression<String> lowerFullName = criteriaBuilder.lower(fullName);

            List<Predicate> predicates = new ArrayList<>();

            for (String word : words) {
                // Condición A: La palabra está en el nombre (LIKE normal)
                Predicate exactLike = criteriaBuilder.like(lowerFullName, "%" + word + "%");

                // Condición B: La palabra "suena" como el nombre o los apellidos (Tolerancia a typos)
                Predicate soundexNombres = criteriaBuilder.equal(
                        criteriaBuilder.function("SOUNDEX", String.class, root.get("name")),
                        criteriaBuilder.function("SOUNDEX", String.class, criteriaBuilder.literal(word))
                );

                Predicate soundexApPaterno = criteriaBuilder.equal(
                        criteriaBuilder.function("SOUNDEX", String.class, root.get("apat")),
                        criteriaBuilder.function("SOUNDEX", String.class, criteriaBuilder.literal(word))
                );

                Predicate soundexApMaterno = criteriaBuilder.equal(
                        criteriaBuilder.function("SOUNDEX", String.class, root.get("amat")),
                        criteriaBuilder.function("SOUNDEX", String.class, criteriaBuilder.literal(word))
                );

                // Agrupamos: O coincide textualmente, O suena igual a alguna parte
                Predicate wordMatch = criteriaBuilder.or(exactLike, soundexNombres, soundexApPaterno, soundexApMaterno);

                predicates.add(wordMatch);
            }

            // Exigimos que todas las palabras ingresadas cumplan la condición (AND)
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
