package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.DepartmentCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.DepartmentResponse;
import com.Chrianto.TicketingSystem.entity.Department;
import com.Chrianto.TicketingSystem.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;

    public DepartmentResponse createDepartment(DepartmentCreateRequest req){
        Department department = new Department();

        department.setName(req.getName());

        department = departmentRepository.save(department);

        return toResponse(department);
    }

    private DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .build();
    }
}
