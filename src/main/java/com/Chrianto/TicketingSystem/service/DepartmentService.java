package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.DepartmentCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.DepartmentResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.Department;
import com.Chrianto.TicketingSystem.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

    public List<DepartmentResponse> getAllDepartments() {
        return departmentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .name(d.getName())
                .build();
    }

}
