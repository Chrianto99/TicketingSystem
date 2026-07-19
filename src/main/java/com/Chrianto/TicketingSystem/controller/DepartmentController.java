package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.DepartmentCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.DepartmentResponse;
import com.Chrianto.TicketingSystem.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentResponse> createDepartment(DepartmentCreateRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.createDepartment(req));

    }

    @GetMapping
    public ResponseEntity<List<DepartmentResponse>> getAllDepartments(){
        return ResponseEntity.ok(departmentService.getAllDepartments());

    }
}
