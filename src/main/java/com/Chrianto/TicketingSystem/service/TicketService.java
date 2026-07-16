package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketResolveRequest;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.*;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final TicketHistoryRepository historyRepository;
    private final CommentRepository commentRepository;

    public TicketResponse createTicket(TicketCreateRequest req) {
        User creator = userRepository.findById(req.getCreatorId())
                .orElseThrow(() -> new EntityNotFoundException("Creator not found"));

        User assignee = userRepository.findById(req.getAssignedUserId())
                .orElseThrow(() -> new EntityNotFoundException("Assignee not found"));

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        Ticket ticket = new Ticket();
        ticket.setCreator(creator);
        ticket.setAssignedUser(assignee);
        ticket.setLastModifiedBy(creator);
        ticket.setDepartment(department);
        ticket.setPhoneNumber(req.getPhoneNumber());
        ticket.setIpAddress(req.getIpAddress());
        ticket.setDescription(req.getDescription());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(req.getPriority()); // sensible default until you add it to the request
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        Comment comment = null;

        if (req.getCommentText() != null && !req.getCommentText().isBlank()) {
            comment = new Comment();
            comment.setText(req.getCommentText());
            comment.setUser(creator);
            comment.setTicket(ticket);
            comment.setTimestamp(LocalDateTime.now());

            comment = commentRepository.save(comment);
        }

        logHistory(ticket, creator, TicketAction.CREATED, assignee, comment);
        logHistory(ticket, creator, TicketAction.ASSIGNED, assignee, comment);


        return toResponse(ticket);
    }


    public TicketResponse resolveTicket(Long ticketId, TicketResolveRequest req) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

        User performedBy = userRepository.findById(req.getPerformedBy())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + req.getPerformedBy()));

        if (ticket.getAssignedUser() == null || !ticket.getAssignedUser().getId().equals(performedBy.getId())) {
            throw new IllegalStateException("Only the assigned user can resolve this ticket");
        }

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already " + ticket.getStatus());
        }

        Comment comment = new Comment();
        comment.setText(req.getCommentText());
        comment.setUser(performedBy);
        comment.setTicket(ticket);
        comment.setTimestamp(LocalDateTime.now());
        comment = commentRepository.save(comment);

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        logHistory(ticket, performedBy, TicketAction.RESOLVED, null, comment);

        return toResponse(ticket);
    }

    public TicketResponse cancelTicket(Long ticketId, TicketResolveRequest req) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

        User performedBy = userRepository.findById(req.getPerformedBy())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + req.getPerformedBy()));

        if (ticket.getAssignedUser() == null || !ticket.getAssignedUser().getId().equals(performedBy.getId())) {
            throw new IllegalStateException("Only the assigned user can cancel this ticket");
        }

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already " + ticket.getStatus());
        }

        Comment comment = new Comment();
        comment.setText(req.getCommentText());
        comment.setUser(performedBy);
        comment.setTicket(ticket);
        comment.setTimestamp(LocalDateTime.now());
        comment = commentRepository.save(comment);

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        logHistory(ticket, performedBy, TicketAction.CANCELLED, null, comment);

        return toResponse(ticket);
    }

    public TicketResponse commentOnTicket(Long ticketId, TicketResolveRequest req) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

        User performedBy = userRepository.findById(req.getPerformedBy())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + req.getPerformedBy()));

        if (ticket.getAssignedUser() == null || !ticket.getAssignedUser().getId().equals(performedBy.getId())) {
            throw new IllegalStateException("Only the assigned user can comment");
        }

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already " + ticket.getStatus());
        }

        Comment comment = new Comment();
        comment.setText(req.getCommentText());
        comment.setUser(performedBy);
        comment.setTicket(ticket);
        comment.setTimestamp(LocalDateTime.now());
        comment = commentRepository.save(comment);

        logHistory(ticket, performedBy, TicketAction.COMMENT_ADDED, null, comment);

        return toResponse(ticket);
    }



    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));
        return toResponse(ticket);
    }


    private void logHistory(Ticket ticket, User performedBy, TicketAction ticketAction,
                            User assignedTo, Comment comment) {

        TicketHistory h = new TicketHistory();
        h.setTicket(ticket);
        h.setPerformedBy(performedBy);
        h.setAction(ticketAction);
        h.setTimestamp(LocalDateTime.now());

        switch (ticketAction) {
            case CREATED -> {
                // nothing extra to attach
            }
            case ASSIGNED, REASSIGNED -> {
                if (assignedTo == null) {
                    throw new IllegalArgumentException(ticketAction + " requires an assignedTo user");
                }
                h.setAssignedTo(assignedTo);
                if (comment != null) {
                    h.setComment(comment);
                }
            }
            case RESOLVED -> {
                if (comment != null) {
                    h.setComment(comment);
                }
            }
            case CANCELLED -> {
                if (comment == null) {
                    throw new IllegalArgumentException("CANCELLED requires a comment");
                }
                h.setComment(comment);
            }
            case COMMENT_ADDED -> {
                if (comment == null) {
                    throw new IllegalArgumentException("No comment typed");
                }
                h.setComment(comment);
            }
            default -> throw new IllegalArgumentException("Unhandled TicketAction: " + ticketAction);
        }

        historyRepository.save(h);
    }
    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .creatorId(t.getCreator().getId())
                .creatorUsername(t.getCreator().getUsername())
                .assignedUserId(t.getAssignedUser() != null ? t.getAssignedUser().getId() : null)
                .assignedUsername(t.getAssignedUser() != null ? t.getAssignedUser().getUsername() : null)
                .departmentId(t.getDepartment().getId())
                .departmentName(t.getDepartment().getName())
                .status(t.getStatus())
                .priority(t.getPriority())
                .phoneNumber(t.getPhoneNumber())
                .ipAddress(t.getIpAddress())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
