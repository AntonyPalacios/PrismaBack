package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "TAREA")
@Data
public class Area {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_area")
    private Long id;

    private String name;
    private String abbreviation;
}
