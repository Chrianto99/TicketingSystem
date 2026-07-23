package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.CommentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketChangeStatusRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketReassignRequest;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.*;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;

    private final TicketHistoryService ticketHistoryService;

    public TicketResponse createTicket(TicketCreateRequest req, User creator) {
        User assignee = userRepository.findById(req.getAssignedUserId())
                .orElseThrow(() -> new EntityNotFoundException("Assignee not found"));

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        Subcategory subcategory = req.getSubcategoryId() != null
                ? subcategoryRepository.findById(req.getSubcategoryId())
                        .orElseThrow(() -> new EntityNotFoundException("Subcategory not found"))
                : null;
        validateSubcategoryBelongsToCategory(category, subcategory);

        Ticket ticket = new Ticket();
        ticket.setCreator(creator);
        ticket.setAssignedUser(assignee);
        ticket.setLastModifiedBy(creator);
        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubcategory(subcategory);
        ticket.setCallerName(req.getCallerName());
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

    public TicketResponse editTicket(Long ticketId, TicketUpdateRequest req, User performedBy){

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        Subcategory subcategory = req.getSubcategoryId() != null
                ? subcategoryRepository.findById(req.getSubcategoryId())
                        .orElseThrow(() -> new EntityNotFoundException("Subcategory not found"))
                : null;
        validateSubcategoryBelongsToCategory(category, subcategory);

        if (!ticket.getCreator().getId().equals(performedBy.getId())) {
            throw new IllegalStateException("Only the creator user can edit this ticket info");
        }

        ticket.setLastModifiedBy(performedBy);
        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubcategory(subcategory);
        ticket.setCallerName(req.getCallerName());
        ticket.setPhoneNumber(req.getPhoneNumber());
        ticket.setIpAddress(req.getIpAddress());
        ticket.setDescription(req.getDescription());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        return toResponse(ticket);
    }

    public void editComment(Long commentId, CommentUpdateRequest req, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (comment.getUser() == null || !comment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Only the comment's author can edit it");
        }

        comment.setText(req.getText());
        commentRepository.save(comment);
    }

    public TicketResponse resolveTicket(Long ticketId, TicketChangeStatusRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

//        if (ticket.getAssignedUser() == null || !ticket.getAssignedUser().getId().equals(performedBy.getId())) {
//            throw new IllegalStateException("Only the assigned user can resolve this ticket");
//        }

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



    public TicketResponse cancelTicket(Long ticketId, TicketChangeStatusRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

//        if (ticket.getAssignedUser() == null || !ticket.getAssignedUser().getId().equals(performedBy.getId())) {
//            throw new IllegalStateException("Only the assigned user can cancel this ticket");
//        }

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already " + ticket.getStatus());
        }

        Comment comment = postComment(ticket, performedBy, req.getCommentText());

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.CANCELLED, null, comment);

        return toResponse(ticket);
    }



    public TicketResponse commentOnTicket(Long ticketId, TicketChangeStatusRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

//        if (ticket.getAssignedUser() == null || !ticket.getAssignedUser().getId().equals(performedBy.getId())) {
//            throw new IllegalStateException("Only the assigned user can comment");
//        }

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Ticket is already " + ticket.getStatus());
        }

        Comment comment = postComment(ticket, performedBy, req.getCommentText());

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.COMMENT_ADDED, null, comment);

        return toResponse(ticket);
    }

    public TicketResponse reassignTicket(Long ticketId, TicketReassignRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id: " + ticketId));

        User assignTo =  userRepository.findById(req.getAssignedTo())
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + req.getAssignedTo()));


//        if (ticket.getAssignedUser() == null || !ticket.getAssignedUser().getId().equals(performedBy.getId())) {
//            throw new IllegalStateException("Only the assigned user can reassign this ticket");
//        }

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


    private void validateSubcategoryBelongsToCategory(Category category, Subcategory subcategory) {
        if (subcategory != null && !subcategory.getCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Subcategory does not belong to the selected category");
        }
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

    public Page<TicketResponse> getAllTickets(TicketStatus status, TicketPriority priority, Pageable pageable){
        return ticketRepository.search(status, priority, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void deleteTicket(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Ticket not found with id: " + ticketId);
        }

        ticketHistoryService.deleteByTicketId(ticketId);
        commentRepository.deleteByTicketId(ticketId);
        ticketRepository.deleteById(ticketId);
    }


    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .creatorId(t.getCreator() != null ? t.getCreator().getId() : null)
                .creatorUsername(t.getCreator() != null ? t.getCreator().getUsername() : null)
                .assignedUserId(t.getAssignedUser() != null ? t.getAssignedUser().getId() : null)
                .assignedUsername(t.getAssignedUser() != null ? t.getAssignedUser().getUsername() : null)
                .departmentId(t.getDepartment() != null ? t.getDepartment().getId() : null)
                .departmentName(t.getDepartment() != null ? t.getDepartment().getName() : null)
                .categoryId(t.getCategory() != null ? t.getCategory().getId() : null)
                .category(t.getCategory() != null ? t.getCategory().getName() : null)
                .subcategoryId(t.getSubcategory() != null ? t.getSubcategory().getId() : null)
                .subcategory(t.getSubcategory() != null ? t.getSubcategory().getName() : null)
                .status(t.getStatus())
                .priority(t.getPriority())
                .callerName(t.getCallerName())
                .phoneNumber(t.getPhoneNumber())
                .ipAddress(t.getIpAddress())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }


}
