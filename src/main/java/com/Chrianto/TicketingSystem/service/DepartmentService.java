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
        department.setActive(true);

        department = departmentRepository.save(department);

        return toResponse(department);
    }

    @Transactional
    public DepartmentResponse updateDepartmentName(Long departmentId, String name) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + departmentId));

        department.setName(name);
        department = departmentRepository.save(department);

        return toResponse(department);
    }

    @Transactional
    public void toggleDepartmentActiveState(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + departmentId));

        department.setActive(!department.isActive());
        departmentRepository.save(department);
    }

    @Transactional
    public void deleteDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with id: " + departmentId));

        if (department.isActive()) {
            throw new IllegalStateException("Only deactivated departments can be deleted");
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
                .active(d.isActive())
                .build();
    }

}
