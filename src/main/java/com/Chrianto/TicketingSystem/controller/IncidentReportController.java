package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.IncidentCommentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentReportCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentReportUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentTicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.CommentResponse;
import com.Chrianto.TicketingSystem.dto.response.IncidentReportResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.service.IncidentReportService;
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
public class IncidentReportController {

    private final IncidentReportService incidentReportService;

    @PostMapping
    public ResponseEntity<IncidentReportResponse> createIncidentReport(@Valid @RequestBody IncidentReportCreateRequest req,
                                                                         @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentReportService.createIncidentReport(req, currentUser));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentReportResponse> getIncidentReport(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentReportService.getIncidentReportById(incidentId));
    }

    @GetMapping
    public ResponseEntity<Page<IncidentReportResponse>> getAllIncidentReports(
            @RequestParam(required = false) IncidentStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(incidentReportService.getAllIncidentReports(status, pageable));
    }

    @PutMapping("/{incidentId}")
    public ResponseEntity<IncidentReportResponse> editIncidentReport(@PathVariable Long incidentId,
                                                                       @Valid @RequestBody IncidentReportUpdateRequest req) {
        return ResponseEntity.ok(incidentReportService.editIncidentReport(incidentId, req));
    }

    @PatchMapping("/{incidentId}/close")
    public ResponseEntity<IncidentReportResponse> closeIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentReportService.closeIncident(incidentId));
    }

    @PatchMapping("/{incidentId}/reopen")
    public ResponseEntity<IncidentReportResponse> reopenIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentReportService.reopenIncident(incidentId));
    }

    @PostMapping("/{incidentId}/comments")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long incidentId,
                                                        @Valid @RequestBody IncidentCommentCreateRequest req,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentReportService.addComment(incidentId, req, currentUser));
    }

    @GetMapping("/{incidentId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentReportService.getCommentsForIncident(incidentId));
    }

    @GetMapping("/{incidentId}/tickets")
    public ResponseEntity<List<TicketResponse>> getTicketsForIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(incidentReportService.getTicketsForIncident(incidentId));
    }

    @PostMapping("/{incidentId}/tickets")
    public ResponseEntity<TicketResponse> createTicketFromIncident(@PathVariable Long incidentId,
                                                                     @Valid @RequestBody IncidentTicketCreateRequest req,
                                                                     @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(incidentReportService.createTicketFromIncident(incidentId, req, currentUser));
    }
}
