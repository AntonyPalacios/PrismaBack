package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "TCYCLE")
@Data
public class Cycle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cycle")
    private Long id;

    private String name;

    private Date startDate;

    private Date endDate;

    @OneToMany(mappedBy = "cycle", cascade = CascadeType.ALL, orphanRemoval = true,fetch = FetchType.LAZY)
    private List<StudentStage> students = new ArrayList<>();

}
