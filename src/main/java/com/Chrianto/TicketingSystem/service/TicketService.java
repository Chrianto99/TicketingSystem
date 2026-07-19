package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketChangeStatusRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketReassignRequest;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.*;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CommentRepository commentRepository;
    private final ProblemTypeRepository problemTypeRepository;

    private final TicketHistoryService ticketHistoryService;

    public TicketResponse createTicket(TicketCreateRequest req) {
        User creator = userRepository.findById(req.getCreatorId())
                .orElseThrow(() -> new EntityNotFoundException("Creator not found"));

        User assignee = userRepository.findById(req.getAssignedUserId())
                .orElseThrow(() -> new EntityNotFoundException("Assignee not found"));

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        ProblemType problemType = problemTypeRepository.findById(req.getProblemTypeId())
                .orElseThrow(() -> new EntityNotFoundException("ProblemType not found"));

        Ticket ticket = new Ticket();
        ticket.setCreator(creator);
        ticket.setAssignedUser(assignee);
        ticket.setLastModifiedBy(creator);
        ticket.setDepartment(department);
        ticket.setProblemType(problemType);
        ticket.setPhoneNumber(req.getPhoneNumber());
        ticket.setIpAddress(req.getIpAddress());
        ticket.setDescription(req.getDescription());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(req.getPriority());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        Comment comment = postComment(ticket, creator, req.getCommentText());

        ticketHistoryService.logHistory(ticket, creator, TicketAction.CREATED, null, null);
        ticketHistoryService.logHistory(ticket, creator, TicketAction.ASSIGNED, assignee, comment);


        return toResponse(ticket);
    }

    public TicketResponse resolveTicket(Long ticketId, TicketChangeStatusRequest req) {
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

        Comment comment = postComment(ticket, performedBy, req.getCommentText());

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.RESOLVED, null, comment);

        return toResponse(ticket);
    }



    public TicketResponse cancelTicket(Long ticketId, TicketChangeStatusRequest req) {
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

        Comment comment = postComment(ticket, performedBy, req.getCommentText());

        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.CANCELLED, null, comment);

        return toResponse(ticket);
    }



    public TicketResponse commentOnTicket(Long ticketId, TicketChangeStatusRequest req) {
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

        Comment comment = postComment(ticket, performedBy, req.getCommentText());

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.COMMENT_ADDED, null, comment);

        return toResponse(ticket);
    }

    public TicketResponse reassignTicket(Long ticketId, TicketReassignRequest req) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

        User performedBy = userRepository.findById(req.getPerformedBy())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + req.getPerformedBy()));

        User assignTo =  userRepository.findById(req.getAssignedTo())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + req.getAssignedTo()));


        if (ticket.getAssignedUser() == null || !ticket.getAssignedUser().getId().equals(performedBy.getId())) {
            throw new IllegalStateException("Only the assigned user can reassign this ticket");
        }

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already " + ticket.getStatus());
        }

        Comment comment = postComment(ticket, performedBy, req.getCommentText());

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setAssignedUser(assignTo);
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.REASSIGNED, assignTo, comment);

        return toResponse(ticket);
    }


    public Comment postComment(Ticket ticket, User performedBy, String commentText){
        if (commentText == null || commentText.isEmpty()) {
            throw new IllegalArgumentException("User must type a comment");
        }
        Comment comment = new Comment();
        comment.setText(commentText);
        comment.setUser(performedBy);
        comment.setTicket(ticket);
        comment.setTimestamp(LocalDateTime.now());
        comment = commentRepository.save(comment);

        return comment;

    }

    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));
        return toResponse(ticket);
    }

    public List<TicketResponse> getAllTickets(){
        return ticketRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
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
                .problemTypeId(t.getProblemType().getId())
                .problemType(t.getProblemType().getName())
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
