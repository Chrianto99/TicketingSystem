package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.ProblemTypeRequest;
import com.Chrianto.TicketingSystem.dto.response.ProblemTypeResponse;
import com.Chrianto.TicketingSystem.service.ProblemTypeService;
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
@RequestMapping("/api/problemTypes")
@RequiredArgsConstructor
public class ProblemTypeController {

    private final ProblemTypeService problemTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProblemTypeResponse> createProblemType(ProblemTypeRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(problemTypeService.createProblemType(req));

    }

    @GetMapping
    public ResponseEntity<List<ProblemTypeResponse>> getAllProblemTypes(){
        return ResponseEntity.ok(problemTypeService.getAllProblemTypes());
    }
}
