package pe.com.edu.prismaapp.prisma.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "TCYCLE")
@Data
public class Cycle implements Comparable<Cycle> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cycle")
    private Long id;

    @Column(name = "name",nullable = false)
    private String name;

    @Column(name = "start_date",nullable = false)
    private Date startDate;

    private Date endDate;
    private boolean current;

    @OneToMany(mappedBy = "cycle",fetch = FetchType.LAZY)
    private List<Stage> stages;

    //ordena por fecha de inicio, descendente
    @Override
    public int compareTo(Cycle o) {
        if(this.startDate.before(o.getStartDate())) return -1;
        if(this.startDate.after(o.getStartDate())) return 1;
        return 0;
    }
}
