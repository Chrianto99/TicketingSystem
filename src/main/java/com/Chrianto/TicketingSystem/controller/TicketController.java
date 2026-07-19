package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketChangeStatusRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketReassignRequest;
import com.Chrianto.TicketingSystem.dto.response.TicketHistoryResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.service.TicketHistoryService;
import com.Chrianto.TicketingSystem.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketHistoryService ticketHistoryService;
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(req));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }


    @PatchMapping("/{ticketId}/resolve")
    public ResponseEntity<TicketResponse> resolveTicket(@PathVariable Long ticketId,
                                                        @Valid @RequestBody TicketChangeStatusRequest req) {
        return ResponseEntity.ok(ticketService.resolveTicket(ticketId, req));
    }

    @PatchMapping("/{ticketId}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(@PathVariable Long ticketId,
                                                        @Valid @RequestBody TicketChangeStatusRequest req) {
        return ResponseEntity.ok(ticketService.cancelTicket(ticketId, req));
    }

    @PostMapping("{ticketId}/comments")
    public ResponseEntity<TicketResponse> commentOnTicket(@PathVariable Long ticketId,
                                                       @Valid @RequestBody TicketChangeStatusRequest req) {
        return ResponseEntity.ok(ticketService.commentOnTicket(ticketId, req));
    }

    @PatchMapping("/{ticketId}/reassign")
    public ResponseEntity<TicketResponse> reassignTicket(@PathVariable Long ticketId,
                                                         @Valid @RequestBody TicketReassignRequest req) {
        return ResponseEntity.ok(ticketService.reassignTicket(ticketId, req));
    }

    @GetMapping("/{ticketId}/history")
    public ResponseEntity<List<TicketHistoryResponse>> getTicketHistory(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketHistoryService.getTicketHistory(ticketId));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }
}
