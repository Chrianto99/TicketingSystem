package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.CallbackTicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketChangeStatusRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketOfferRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketReassignRequest;
import com.Chrianto.TicketingSystem.dto.response.CommentResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketHistoryResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketSource;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.service.TicketHistoryService;
import com.Chrianto.TicketingSystem.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final TicketHistoryService ticketHistoryService;
    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody TicketCreateRequest req,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(req, currentUser));
    }

    @PostMapping("/callback")
    public ResponseEntity<TicketResponse> createCallbackTicket(@Valid @RequestBody CallbackTicketCreateRequest req,
                                                                 @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createCallbackTicket(req, currentUser));
    }

    @PatchMapping("/{ticketId}/resolve-callback")
    public ResponseEntity<Void> resolveCallbackTicket(@PathVariable Long ticketId) {
        ticketService.resolveCallbackTicket(ticketId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> editTicket(@PathVariable Long ticketId,
                                                     @Valid @RequestBody TicketUpdateRequest req,
                                                     @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ticketService.editTicket(ticketId, req, currentUser));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }


    @PatchMapping("/{ticketId}/resolve")
    public ResponseEntity<TicketResponse> resolveTicket(@PathVariable Long ticketId,
                                                        @Valid @RequestBody TicketChangeStatusRequest req,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ticketService.resolveTicket(ticketId, req, currentUser));
    }

    @PatchMapping("/{ticketId}/cancel")
    public ResponseEntity<TicketResponse> cancelTicket(@PathVariable Long ticketId,
                                                        @Valid @RequestBody TicketChangeStatusRequest req,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ticketService.cancelTicket(ticketId, req, currentUser));
    }

    @PatchMapping("/{ticketId}/reopen")
    public ResponseEntity<TicketResponse> reopenTicket(@PathVariable Long ticketId,
                                                        @Valid @RequestBody TicketChangeStatusRequest req,
                                                        @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ticketService.reopenTicket(ticketId, req, currentUser));
    }

    @PostMapping("{ticketId}/comments")
    public ResponseEntity<TicketResponse> commentOnTicket(@PathVariable Long ticketId,
                                                       @Valid @RequestBody TicketChangeStatusRequest req,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ticketService.commentOnTicket(ticketId, req, currentUser));
    }

    @GetMapping("/{ticketId}/comments")
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getCommentsForTicket(ticketId));
    }

    @DeleteMapping("/{ticketId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long ticketId,
                                               @PathVariable Long commentId,
                                               @AuthenticationPrincipal User currentUser) {
        ticketService.deleteComment(commentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{ticketId}/reassign")
    public ResponseEntity<TicketResponse> reassignTicket(@PathVariable Long ticketId,
                                                         @Valid @RequestBody TicketReassignRequest req,
                                                         @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ticketService.reassignTicket(ticketId, req, currentUser));
    }

    @PostMapping("/{ticketId}/offer")
    public ResponseEntity<TicketResponse> offerTicket(@PathVariable Long ticketId,
                                                       @Valid @RequestBody TicketOfferRequest req,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ticketService.offerTicketToCandidates(ticketId, req.getCandidateUserIds(), currentUser));
    }

    @PatchMapping("/{ticketId}/claim")
    public ResponseEntity<TicketResponse> claimTicket(@PathVariable Long ticketId,
                                                       @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(ticketService.claimTicket(ticketId, currentUser));
    }

    @GetMapping("/{ticketId}/history")
    public ResponseEntity<List<TicketHistoryResponse>> getTicketHistory(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketHistoryService.getTicketHistory(ticketId));
    }

    @GetMapping
    public ResponseEntity<Page<TicketResponse>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) Long createdByUserId,
            @RequestParam(required = false) Long assignedToUserId,
            @RequestParam(required = false) TicketSource source,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ticketService.getAllTickets(status, priority, createdByUserId, assignedToUserId, source, query, pageable));
    }

    @DeleteMapping("/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long ticketId) {
        ticketService.deleteTicket(ticketId);
        return ResponseEntity.noContent().build();
    }
}
