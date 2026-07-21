package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.DepartmentCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.DepartmentResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.Department;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.DepartmentRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentService {
    private final DepartmentRepository departmentRepository;
    private final TicketRepository ticketRepository;

    public DepartmentResponse createDepartment(DepartmentCreateRequest req){
        Department department = new Department();

        department.setName(req.getName());

        department = departmentRepository.save(department);

        return toResponse(department);
    }

    @Transactional
    public void deleteDepartment(Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            throw new EntityNotFoundException("Department not found with id: " + departmentId);
        }

        ticketRepository.nullifyDepartment(departmentId);
        departmentRepository.deleteById(departmentId);
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
