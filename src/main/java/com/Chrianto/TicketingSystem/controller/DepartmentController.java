package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.DepartmentCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.DepartmentResponse;
import com.Chrianto.TicketingSystem.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> createDepartment(@Valid @RequestBody DepartmentCreateRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(req));

    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments(){
        return ResponseEntity.ok(departmentService.getAllDepartments());

    }

    @DeleteMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long departmentId) {
        departmentService.deleteDepartment(departmentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> updateDepartment(@PathVariable Long departmentId,
                                                                 @Valid @RequestBody DepartmentCreateRequest req) {
        return ResponseEntity.ok(departmentService.updateDepartment(departmentId, req.getName(), req.getCode()));
    }

    @PatchMapping("/{departmentId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleDepartmentActive(@PathVariable Long departmentId) {
        departmentService.toggleDepartmentActiveState(departmentId);
        return ResponseEntity.noContent().build();
    }
}
