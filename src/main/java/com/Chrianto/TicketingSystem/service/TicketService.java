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
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
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
    private final AttachmentService attachmentService;
    private final NotificationService notificationService;

    public TicketResponse createTicket(TicketCreateRequest req, User creator) {
        User assignee = userRepository.findById(req.getAssignedUserId())
                .orElseThrow(() -> new EntityNotFoundException("Ο ανάδοχος χρήστης δεν βρέθηκε"));

        Department department = req.getDepartmentId() != null
                ? departmentRepository.findById(req.getDepartmentId())
                        .orElseThrow(() -> new EntityNotFoundException("Το τμήμα δεν βρέθηκε"))
                : null;

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Η κατηγορία βλάβης δεν βρέθηκε"));

        Subcategory subcategory = req.getSubcategoryId() != null
                ? subcategoryRepository.findById(req.getSubcategoryId())
                        .orElseThrow(() -> new EntityNotFoundException("Η υποκατηγορία δεν βρέθηκε"))
                : null;
        validateSubcategoryBelongsToCategory(category, subcategory);

        boolean autoResolve = req.getResolution() != null && !req.getResolution().isBlank();
        if (autoResolve && subcategory == null) {
            throw new IllegalArgumentException("Απαιτείται υποκατηγορία για την επίλυση ενός ticket");
        }

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
        ticket.setSummary(req.getSummary());
        ticket.setDescription(req.getDescription());
        ticket.setStatus(autoResolve ? TicketStatus.RESOLVED : TicketStatus.OPEN);
        ticket.setPriority(req.getPriority());
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        if (autoResolve) {
            ticket.setResolution(req.getResolution());
        }

        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, creator, TicketAction.CREATED, null, null, null);
        ticketHistoryService.logHistory(ticket, creator, TicketAction.ASSIGNED, assignee, null, null);
        if (autoResolve) {
            ticketHistoryService.logHistory(ticket, creator, TicketAction.RESOLVED, null, null, null);
        }
        if (!assignee.getId().equals(creator.getId())) {
            notificationService.notifyTicketAssigned(assignee.getId());
        }

        return toResponse(ticket);
    }

    public TicketResponse editTicket(Long ticketId, TicketUpdateRequest req, User performedBy){

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        Department department = departmentRepository.findById(req.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Το τμήμα δεν βρέθηκε"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Η κατηγορία βλάβης δεν βρέθηκε"));

        Subcategory subcategory = req.getSubcategoryId() != null
                ? subcategoryRepository.findById(req.getSubcategoryId())
                        .orElseThrow(() -> new EntityNotFoundException("Η υποκατηγορία δεν βρέθηκε"))
                : null;
        validateSubcategoryBelongsToCategory(category, subcategory);

        if (performedBy.getRole() != UserRole.ADMIN && !ticket.getCreator().getId().equals(performedBy.getId())) {
            throw new IllegalStateException("Μόνο ο δημιουργός του ticket μπορεί να επεξεργαστεί αυτές τις πληροφορίες");
        }

        String changes = buildInfoChangeDescription(ticket, req, department, category, subcategory);

        ticket.setLastModifiedBy(performedBy);
        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubcategory(subcategory);
        ticket.setCallerName(req.getCallerName());
        ticket.setPhoneNumber(req.getPhoneNumber());
        ticket.setIpAddress(req.getIpAddress());
        ticket.setSummary(req.getSummary());
        ticket.setDescription(req.getDescription());
        ticket.setPriority(req.getPriority());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        if (changes != null) {
            ticketHistoryService.logHistory(ticket, performedBy, TicketAction.INFO_CHANGED, null, null, changes);
        }

        return toResponse(ticket);
    }

    private static final int HISTORY_VALUE_MAX_LENGTH = 80;

    private String buildInfoChangeDescription(Ticket before, TicketUpdateRequest req,
                                               Department department, Category category, Subcategory subcategory) {
        List<String> changes = new java.util.ArrayList<>();
        appendChange(changes, "Τίτλος", before.getSummary(), req.getSummary());
        appendChange(changes, "Όνομα καλούντος", before.getCallerName(), req.getCallerName());
        appendChange(changes, "Αριθμός τηλεφώνου", before.getPhoneNumber(), req.getPhoneNumber());
        appendChange(changes, "Διεύθυνση IP", before.getIpAddress(), req.getIpAddress());
        appendChange(changes, "Τμήμα",
                before.getDepartment() != null ? before.getDepartment().getName() : null,
                department != null ? department.getName() : null);
        appendChange(changes, "Κατηγορία Βλάβης",
                before.getCategory() != null ? before.getCategory().getName() : null,
                category != null ? category.getName() : null);
        appendChange(changes, "Υποκατηγορία",
                before.getSubcategory() != null ? before.getSubcategory().getName() : null,
                subcategory != null ? subcategory.getName() : null);
        appendChange(changes, "Προτεραιότητα",
                before.getPriority() != null ? before.getPriority().getDisplayName() : null,
                req.getPriority() != null ? req.getPriority().getDisplayName() : null);
        appendChange(changes, "Λεπτομέρειες", before.getDescription(), req.getDescription());

        return changes.isEmpty() ? null : String.join("; ", changes);
    }

    private void appendChange(List<String> changes, String label, String oldValue, String newValue) {
        String oldNorm = oldValue == null ? "" : oldValue.trim();
        String newNorm = newValue == null ? "" : newValue.trim();
        if (oldNorm.equals(newNorm)) {
            return;
        }
        String oldDisplay = oldNorm.isEmpty() ? "—" : truncateForHistory(oldNorm, HISTORY_VALUE_MAX_LENGTH);
        String newDisplay = newNorm.isEmpty() ? "—" : truncateForHistory(newNorm, HISTORY_VALUE_MAX_LENGTH);
        changes.add(label + ": «" + oldDisplay + "» → «" + newDisplay + "»");
    }

    private static final int HISTORY_REASON_MAX_LENGTH = 1900;
    private static final int HISTORY_COMMENT_DIFF_MAX_LENGTH = 900;

    private String truncateForHistory(String value, int maxLength) {
        return value.length() > maxLength
                ? value.substring(0, maxLength) + "…"
                : value;
    }

    public void editComment(Long commentId, CommentUpdateRequest req, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Το σχόλιο δεν βρέθηκε"));

        if (comment.getUser() == null || !comment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Μόνο ο συντάκτης του σχολίου μπορεί να το επεξεργαστεί");
        }

        String oldText = comment.getText();
        comment.setText(req.getText());
        commentRepository.save(comment);

        if (!oldText.trim().equals(req.getText().trim())) {
            String description = "Σχόλιο: «" + truncateForHistory(oldText.trim(), HISTORY_COMMENT_DIFF_MAX_LENGTH) +
                    "» → «" + truncateForHistory(req.getText().trim(), HISTORY_COMMENT_DIFF_MAX_LENGTH) + "»";
            ticketHistoryService.logHistory(comment.getTicket(), currentUser, TicketAction.COMMENT_EDITED, null, null, description);
        }
    }

    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Το σχόλιο δεν βρέθηκε"));

        if (comment.getUser() == null || !comment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Μόνο ο συντάκτης του σχολίου μπορεί να το διαγράψει");
        }

        Ticket ticket = comment.getTicket();
        String commentText = comment.getText();

        ticketHistoryService.unlinkComment(commentId);
        commentRepository.delete(comment);

        ticketHistoryService.logHistory(ticket, currentUser, TicketAction.COMMENT_REMOVED, null, null,
                truncateForHistory(commentText.trim(), HISTORY_COMMENT_DIFF_MAX_LENGTH));
    }

    public TicketResponse resolveTicket(Long ticketId, TicketChangeStatusRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));


        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Το ticket είναι ήδη " + ticket.getStatus().getDisplayName());
        }

        if (req.getSubcategoryId() != null) {
            Subcategory subcategory = subcategoryRepository.findById(req.getSubcategoryId())
                    .orElseThrow(() -> new EntityNotFoundException("Η υποκατηγορία δεν βρέθηκε"));
            validateSubcategoryBelongsToCategory(ticket.getCategory(), subcategory);
            ticket.setSubcategory(subcategory);
        }

        if (ticket.getSubcategory() == null) {
            throw new IllegalArgumentException("Απαιτείται υποκατηγορία για την επίλυση ενός ticket");
        }

        ticket.setResolution(req.getCommentText());
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.RESOLVED, null, null, null);

        return toResponse(ticket);
    }



    public TicketResponse cancelTicket(Long ticketId, TicketChangeStatusRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));


        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Το ticket είναι ήδη " + ticket.getStatus().getDisplayName());
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.CANCELLED, null, null,
                truncateForHistory(req.getCommentText().trim(), HISTORY_REASON_MAX_LENGTH));

        return toResponse(ticket);
    }



    public TicketResponse commentOnTicket(Long ticketId, TicketChangeStatusRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));


        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Το ticket είναι ήδη " + ticket.getStatus().getDisplayName());
        }

        Comment comment = postComment(ticket, performedBy, req.getCommentText());

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.COMMENT_ADDED, null, comment, null);

        return toResponse(ticket);
    }

    public TicketResponse reassignTicket(Long ticketId, TicketReassignRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        User assignTo =  userRepository.findById(req.getAssignedTo())
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + req.getAssignedTo()));


        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new IllegalStateException("Το ticket είναι ήδη " + ticket.getStatus().getDisplayName());
        }

        String reason = req.getCommentText() != null && !req.getCommentText().isBlank()
                ? truncateForHistory(req.getCommentText().trim(), HISTORY_REASON_MAX_LENGTH)
                : null;

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket.setAssignedUser(assignTo);
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.REASSIGNED, assignTo, null, reason);

        if (!assignTo.getId().equals(performedBy.getId())) {
            notificationService.notifyTicketAssigned(assignTo.getId());
        }

        return toResponse(ticket);
    }

    public TicketResponse reopenTicket(Long ticketId, TicketChangeStatusRequest req, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));


        if (ticket.getStatus() == TicketStatus.OPEN) {
            throw new IllegalStateException("Το ticket είναι ήδη " + ticket.getStatus().getDisplayName());
        }

        ticket.setStatus(TicketStatus.OPEN);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.REOPENED, null, null,
                truncateForHistory(req.getCommentText().trim(), HISTORY_REASON_MAX_LENGTH));

        return toResponse(ticket);
    }




    private void validateSubcategoryBelongsToCategory(Category category, Subcategory subcategory) {
        if (subcategory != null && !subcategory.getCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Η υποκατηγορία δεν ανήκει στην επιλεγμένη κατηγορία βλάβης");
        }
    }

    public Comment postComment(Ticket ticket, User performedBy, String commentText){
        if (commentText == null || commentText.isEmpty()) {
            throw new IllegalArgumentException("Ο χρήστης πρέπει να πληκτρολογήσει ένα σχόλιο");
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
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));
        return toResponse(ticket);
    }

    public Page<TicketResponse> getAllTickets(TicketStatus status, TicketPriority priority, Long createdByUserId,
                                               Long assignedToUserId, String query, Pageable pageable){
        return ticketRepository.search(status, priority, createdByUserId, assignedToUserId, query, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public void deleteTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        if (ticket.getStatus() != TicketStatus.CANCELLED) {
            throw new IllegalStateException("Μόνο ακυρωμένα tickets μπορούν να διαγραφούν");
        }

        attachmentService.deleteAttachmentsForTicket(ticketId);
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
                .summary(t.getSummary())
                .description(t.getDescription())
                .resolution(t.getResolution())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }


}
