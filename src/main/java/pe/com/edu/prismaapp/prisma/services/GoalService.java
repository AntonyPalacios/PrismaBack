package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.GoalApi;

public interface GoalService {

    GoalApi.Response save(GoalApi.Create goalCreate);

    GoalApi.Response update(Long id, GoalApi.Update goalUpdate);

    void delete(Long id);

}
