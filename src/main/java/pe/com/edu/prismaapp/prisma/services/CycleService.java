package pe.com.edu.prismaapp.prisma.services;

import pe.com.edu.prismaapp.prisma.dto.CycleApi;

import java.util.List;

public interface CycleService {

    CycleApi.Response save(CycleApi.Create cycleDto);
    List<CycleApi.Response> findAll();

    CycleApi.Response update(Long id, CycleApi.Update cycle);

    void delete(Long id);

    CycleApi.Response getCurrentCycle();
}
