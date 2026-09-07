package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.IncidentCommentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentTicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.CommentResponse;
import com.Chrianto.TicketingSystem.dto.response.IncidentResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/incident-reports")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(@Valid @RequestBody IncidentCreateRequest req,
                                                             @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.createIncident(req, currentUser));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> getIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentService.getIncidentById(incidentId));
    }

    @GetMapping
    public ResponseEntity<Page<IncidentResponse>> getAllIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(incidentService.getAllIncidents(status, priority, query, pageable));
    }

    @PutMapping("/{incidentId}")
    public ResponseEntity<IncidentResponse> editIncident(@PathVariable Long incidentId,
                                                           @Valid @RequestBody IncidentUpdateRequest req,
                                                           @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(incidentService.editIncident(incidentId, req, currentUser));
    }

    @PatchMapping("/{incidentId}/close")
    public ResponseEntity<IncidentResponse> closeIncident(@PathVariable Long incidentId,
                                                            @Valid @RequestBody IncidentCommentCreateRequest req,
                                                            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(incidentService.closeIncident(incidentId, req, currentUser));
    }

    @PatchMapping("/{incidentId}/reopen")
    public ResponseEntity<IncidentResponse> reopenIncident(@PathVariable Long incidentId,
                                                             @Valid @RequestBody IncidentCommentCreateRequest req,
                                                             @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(incidentService.reopenIncident(incidentId, req, currentUser));
    }

    @PostMapping("/{incidentId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long incidentId,
                                                        @Valid @RequestBody IncidentCommentCreateRequest req,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.addComment(incidentId, req, currentUser));
    }

    @GetMapping("/{incidentId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentService.getCommentsForIncident(incidentId));
    }

    @GetMapping("/{incidentId}/tickets")
    public ResponseEntity<List<TicketResponse>> getTicketsForIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentService.getTicketsForIncident(incidentId));
    }

    @PostMapping("/{incidentId}/tickets")
    public ResponseEntity<TicketResponse> createTicketFromIncident(@PathVariable Long incidentId,
                                                                     @Valid @RequestBody IncidentTicketCreateRequest req,
                                                                     @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentService.createTicketFromIncident(incidentId, req, currentUser));
    }
}
