package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.CallbackTicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.CommentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketChangeStatusRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketReassignRequest;
import com.Chrianto.TicketingSystem.dto.response.CommentResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.*;
import com.Chrianto.TicketingSystem.entity.enums.*;
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
        if (req.getCandidateUserIds() == null || req.getCandidateUserIds().isEmpty()) {
            throw new IllegalArgumentException("Απαιτείται τουλάχιστον ένας υποψήφιος χρήστης");
        }

        List<User> candidates = userRepository.findAllById(req.getCandidateUserIds());
        if (candidates.size() != new java.util.HashSet<>(req.getCandidateUserIds()).size()) {
            throw new EntityNotFoundException("Ένας ή περισσότεροι υποψήφιοι χρήστες δεν βρέθηκαν");
        }
        if (candidates.stream().anyMatch(u -> !u.isActive())) {
            throw new IllegalArgumentException("Δεν μπορείτε να προσφέρετε το ticket σε απενεργοποιημένο χρήστη");
        }

        Department department = null;
        if (req.getDepartmentName() != null && !req.getDepartmentName().isBlank()) {
            String departmentName = req.getDepartmentName().trim();
            department = departmentRepository.findByNameIgnoreCase(departmentName)
                    .orElseGet(() -> {
                        Department newDepartment = new Department();
                        newDepartment.setName(departmentName);
                        newDepartment.setActive(true);
                        return departmentRepository.save(newDepartment);
                    });
        }

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
        // A resolved ticket needs someone credited with resolving it right away —
        // there's no "claim" step for something that's already done — so this is
        // one path where a candidate becomes a direct assignee instead of an offer.
        if (autoResolve && candidates.size() != 1) {
            throw new IllegalArgumentException("Ένα επιλυμένο ticket πρέπει να έχει ακριβώς έναν χρήστη");
        }
        // The other: offering it to no one but yourself is just claiming it —
        // there's no one else who could also grab it, so skip the offer/claim
        // round-trip and assign it directly.
        boolean selfClaim = !autoResolve && candidates.size() == 1 && candidates.get(0).getId().equals(creator.getId());
        User directAssignee = autoResolve || selfClaim ? candidates.get(0) : null;

        Ticket ticket = new Ticket();
        ticket.setCreator(creator);
        if (directAssignee != null) {
            ticket.setAssignedUser(directAssignee);
        } else {
            ticket.getCandidates().addAll(candidates);
        }
        ticket.setLastModifiedBy(creator);
        ticket.setDepartment(department);
        ticket.setCategory(category);
        ticket.setSubcategory(subcategory);
        ticket.setCallerName(req.getCallerName());
        ticket.setPhoneNumber(req.getPhoneNumber());
        ticket.setIpAddress(req.getIpAddress());
        // No title typed: fall back to "Category - Department" instead of
        // leaving every untitled ticket looking identical in the list.
        String summary = (req.getSummary() == null || req.getSummary().isBlank())
                ? category.getName() + (department != null ? " - " + department.getName() : "")
                : req.getSummary();
        ticket.setSummary(summary);
        ticket.setDescription(req.getDescription());
        ticket.setStatus(autoResolve ? TicketStatus.RESOLVED : TicketStatus.OPEN);
        ticket.setPriority(req.getPriority());
        ticket.setSource(TicketSource.MANUAL);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());
        if (autoResolve) {
            ticket.setResolution(req.getResolution());
        }

        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, creator, TicketAction.CREATED, null, null, null);
        if (selfClaim) {
            ticketHistoryService.logHistory(ticket, creator, TicketAction.CLAIMED, null, null, null);
        } else if (directAssignee != null) {
            ticketHistoryService.logHistory(ticket, creator, TicketAction.ASSIGNED, directAssignee, null, null);
            ticketHistoryService.logHistory(ticket, creator, TicketAction.RESOLVED, null, null, null);
            if (!directAssignee.getId().equals(creator.getId())) {
                notificationService.notifyTicketAssigned(directAssignee.getId());
            }
        } else {
            String names = candidates.stream().map(User::getUsername).collect(java.util.stream.Collectors.joining(", "));
            String sentence = "Ο χρήστης " + creator.getUsername() + " πρόσφερε το ticket σε: " + names + ".";
            ticketHistoryService.logHistory(ticket, creator, TicketAction.OFFERED, null, null, sentence);
            for (User candidate : candidates) {
                if (!candidate.getId().equals(creator.getId())) {
                    notificationService.notifyTicketAssigned(candidate.getId());
                }
            }
        }

        return toResponse(ticket);
    }

    public TicketResponse createCallbackTicket(CallbackTicketCreateRequest req, User creator) {
        if (req.getCandidateUserIds() == null || req.getCandidateUserIds().isEmpty()) {
            throw new IllegalArgumentException("Απαιτείται τουλάχιστον ένας υποψήφιος χρήστης");
        }

        List<User> candidates = userRepository.findAllById(req.getCandidateUserIds());
        if (candidates.size() != new java.util.HashSet<>(req.getCandidateUserIds()).size()) {
            throw new EntityNotFoundException("Ένας ή περισσότεροι υποψήφιοι χρήστες δεν βρέθηκαν");
        }
        if (candidates.stream().anyMatch(u -> !u.isActive())) {
            throw new IllegalArgumentException("Δεν μπορείτε να προσφέρετε το ticket σε απενεργοποιημένο χρήστη");
        }
        // Offering it to no one but yourself is just claiming it.
        boolean selfClaim = candidates.size() == 1 && candidates.get(0).getId().equals(creator.getId());

        Ticket ticket = new Ticket();
        ticket.setCreator(creator);
        if (selfClaim) {
            ticket.setAssignedUser(creator);
        } else {
            ticket.getCandidates().addAll(candidates);
        }
        ticket.setLastModifiedBy(creator);
        ticket.setCallerName(req.getCallerName());
        ticket.setPhoneNumber(req.getPhoneNumber());
        ticket.setDescription(req.getDescription());
        ticket.setSummary("Επιστροφή κλήσης σε " + req.getCallerName());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(req.getPriority());
        ticket.setSource(TicketSource.CALLBACK);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, creator, TicketAction.CREATED, null, null, null);
        if (selfClaim) {
            ticketHistoryService.logHistory(ticket, creator, TicketAction.CLAIMED, null, null, null);
        } else {
            String names = candidates.stream().map(User::getUsername).collect(java.util.stream.Collectors.joining(", "));
            String sentence = "Ο χρήστης " + creator.getUsername() + " πρόσφερε το ticket σε: " + names + ".";
            ticketHistoryService.logHistory(ticket, creator, TicketAction.OFFERED, null, null, sentence);
            for (User candidate : candidates) {
                if (!candidate.getId().equals(creator.getId())) {
                    notificationService.notifyTicketAssigned(candidate.getId());
                }
            }
        }

        return toResponse(ticket);
    }

    // Callback tickets have no resolution step — "resolving" one just means the call
    // was made, so it's deleted outright rather than marked RESOLVED.
    @Transactional
    public void resolveCallbackTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        if (ticket.getSource() != TicketSource.CALLBACK) {
            throw new IllegalStateException("Μόνο tickets τύπου επιστροφής κλήσης μπορούν να ολοκληρωθούν με αυτόν τον τρόπο");
        }

        purgeTicket(ticketId);
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

        if (ticket.getSubcategory() == null && ticket.getSource() != TicketSource.INCIDENT) {
            throw new IllegalArgumentException("Απαιτείται υποκατηγορία για την επίλυση ενός ticket");
        }

        boolean hasResolutionText = req.getCommentText() != null && !req.getCommentText().isBlank();
        ticket.setResolution(hasResolutionText ? req.getCommentText() : null);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        // An offered-but-unclaimed (or even unassigned) ticket can still be
        // resolved directly — resolving it is what makes the resolver the
        // assignee, same as claiming would have. Clear any leftover candidates
        // so it doesn't keep rendering as claimable/yellow.
        if (ticket.getAssignedUser() == null) {
            ticket.setAssignedUser(performedBy);
        }
        ticket.getCandidates().clear();
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.RESOLVED, null, null,
                hasResolutionText ? truncateForHistory(req.getCommentText().trim(), HISTORY_REASON_MAX_LENGTH) : null);
        logIncidentCrossPost(ticket, performedBy, IncidentAction.TICKET_RESOLVED,
                "Ο χρήστης " + performedBy.getUsername() + " επέλυσε το ticket #" + ticket.getId() + " («" + ticket.getSummary() + "»).");

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
        ticket.getCandidates().clear();
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.CANCELLED, null, null, optionalReason(req));
        logIncidentCrossPost(ticket, performedBy, IncidentAction.TICKET_CANCELLED,
                "Ο χρήστης " + performedBy.getUsername() + " ακύρωσε το ticket #" + ticket.getId() + " («" + ticket.getSummary() + "»).");

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

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.REOPENED, null, null, optionalReason(req));
        logIncidentCrossPost(ticket, performedBy, IncidentAction.TICKET_REOPENED,
                "Ο χρήστης " + performedBy.getUsername() + " επανάνοιξε το ticket #" + ticket.getId() + " («" + ticket.getSummary() + "»).");

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
        // A direct reassign supersedes any open multi-candidate offer — otherwise
        // the stale candidate rows could make the ticket look claimable again
        // later (e.g. if this assignee is ever deleted and the FK nulls out).
        ticket.getCandidates().clear();
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.REASSIGNED, assignTo, null, reason);
        logIncidentCrossPost(ticket, performedBy, IncidentAction.TICKET_ASSIGNED,
                "Ο χρήστης " + performedBy.getUsername() + " ανέθεσε το ticket #" + ticket.getId() + " («" + ticket.getSummary() + "») σε " + assignTo.getUsername() + ".");

        if (!assignTo.getId().equals(performedBy.getId())) {
            notificationService.notifyTicketAssigned(assignTo.getId());
        }

        return toResponse(ticket);
    }

    // Puts the ticket up for grabs among several candidates instead of a single
    // assignee — assignedUser is cleared, so the ticket reads as "unassigned but
    // claimable" until one of them calls claimTicket().
    @Transactional
    public TicketResponse offerTicketToCandidates(Long ticketId, List<Long> candidateUserIds, User performedBy) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        if (ticket.getStatus() != TicketStatus.OPEN) {
            throw new IllegalStateException("Το ticket είναι ήδη " + ticket.getStatus().getDisplayName());
        }
        if (candidateUserIds == null || candidateUserIds.isEmpty()) {
            throw new IllegalArgumentException("Απαιτείται τουλάχιστον ένας υποψήφιος χρήστης");
        }

        List<User> candidates = userRepository.findAllById(candidateUserIds);
        if (candidates.size() != new java.util.HashSet<>(candidateUserIds).size()) {
            throw new EntityNotFoundException("Ένας ή περισσότεροι υποψήφιοι χρήστες δεν βρέθηκαν");
        }
        if (candidates.stream().anyMatch(u -> !u.isActive())) {
            throw new IllegalArgumentException("Δεν μπορείτε να προσφέρετε το ticket σε απενεργοποιημένο χρήστη");
        }
        // Offering it to no one but yourself is just claiming it — skip the
        // offer/claim round-trip and assign it directly.
        boolean selfClaim = candidates.size() == 1 && candidates.get(0).getId().equals(performedBy.getId());

        ticket.setAssignedUser(selfClaim ? performedBy : null);
        ticket.getCandidates().clear();
        if (!selfClaim) {
            ticket.getCandidates().addAll(candidates);
        }
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        if (selfClaim) {
            ticketHistoryService.logHistory(ticket, performedBy, TicketAction.CLAIMED, null, null, null);
            logIncidentCrossPost(ticket, performedBy, IncidentAction.TICKET_ASSIGNED,
                    "Ο χρήστης " + performedBy.getUsername() + " ανέλαβε το ticket #" + ticket.getId() +
                            " («" + ticket.getSummary() + "»).");
        } else {
            String names = candidates.stream().map(User::getUsername).collect(java.util.stream.Collectors.joining(", "));
            String sentence = "Ο χρήστης " + performedBy.getUsername() + " πρόσφερε το ticket σε: " + names + ".";
            ticketHistoryService.logHistory(ticket, performedBy, TicketAction.OFFERED, null, null, sentence);
            logIncidentCrossPost(ticket, performedBy, IncidentAction.TICKET_ASSIGNED,
                    "Ο χρήστης " + performedBy.getUsername() + " πρόσφερε το ticket #" + ticket.getId() +
                            " («" + ticket.getSummary() + "») σε: " + names + ".");

            for (User candidate : candidates) {
                if (!candidate.getId().equals(performedBy.getId())) {
                    notificationService.notifyTicketAssigned(candidate.getId());
                }
            }
        }

        return toResponse(ticket);
    }

    // Race-safe: the atomic UPDATE in claimIfCandidate is the single source of
    // truth for who wins when multiple candidates claim at once. Everything
    // after it just cleans up the now-settled ticket.
    @Transactional
    public TicketResponse claimTicket(Long ticketId, User performedBy) {
        int claimed = ticketRepository.claimIfCandidate(ticketId, performedBy.getId());
        if (claimed == 0) {
            throw new IllegalStateException("Το ticket δεν είναι πλέον διαθέσιμο για ανάληψη");
        }
        ticketRepository.clearCandidates(ticketId);

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));
        ticket.setLastModifiedBy(performedBy);
        ticket.setUpdatedAt(LocalDateTime.now());
        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, performedBy, TicketAction.CLAIMED, null, null, null);
        logIncidentCrossPost(ticket, performedBy, IncidentAction.TICKET_ASSIGNED,
                "Ο χρήστης " + performedBy.getUsername() + " ανέλαβε το ticket #" + ticket.getId() +
                        " («" + ticket.getSummary() + "»).");

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

        commentRepository.delete(comment);

        ticketHistoryService.logHistory(ticket, currentUser, TicketAction.COMMENT_REMOVED, null, null,
                truncateForHistory(commentText.trim(), HISTORY_COMMENT_DIFF_MAX_LENGTH));
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

    // Cancel/reopen no longer require a reason — null here just means the
    // history entry for this event won't show a quote line.
    private String optionalReason(TicketChangeStatusRequest req) {
        return req.getCommentText() != null && !req.getCommentText().isBlank()
                ? truncateForHistory(req.getCommentText().trim(), HISTORY_REASON_MAX_LENGTH)
                : null;
    }



    // Cross-posts a ticket-lifecycle event onto its parent incident's timeline,
    // for tickets that were spawned from one (source == INCIDENT).
    private void logIncidentCrossPost(Ticket ticket, User performedBy, IncidentAction action, String description) {
        if (ticket.getSource() == TicketSource.INCIDENT && ticket.getIncident() != null) {
            ticketHistoryService.logIncidentHistory(ticket.getIncident(), performedBy, action, null, description);
        }
    }


    private void validateSubcategoryBelongsToCategory(Category category, Subcategory subcategory) {
        if (subcategory != null && !subcategory.getCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("Η υποκατηγορία δεν ανήκει στην επιλεγμένη κατηγορία βλάβης");
        }
    }


    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));
        return toResponse(ticket);
    }

    public Page<TicketResponse> getAllTickets(TicketStatus status, TicketPriority priority, Long createdByUserId,
                                              Long assignedToUserId, TicketSource source, String query, Pageable pageable){
        return ticketRepository.search(status, priority, createdByUserId, assignedToUserId, source, query, pageable)
                .map(this::toResponse);
    }

    // "My Tickets" scope: assigned to me OR offered to me as an unclaimed candidate.
    public Page<TicketResponse> getTicketsAssignedOrCandidate(Long userId, TicketStatus status, TicketPriority priority,
                                                               TicketSource source, String query, Pageable pageable) {
        return ticketRepository.searchAssignedOrCandidate(status, priority, userId, source, query, pageable)
                .map(this::toResponse);
    }

    public List<CommentResponse> getCommentsForTicket(Long ticketId) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));
        return commentRepository.findByTicketIdOrderByTimestampAscIdAsc(ticketId).stream()
                .map(this::toCommentResponse)
                .toList();
    }



    @Transactional
    public void deleteTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        if (ticket.getStatus() != TicketStatus.CANCELLED) {
            throw new IllegalStateException("Μόνο ακυρωμένα tickets μπορούν να διαγραφούν");
        }

        purgeTicket(ticketId);
    }

    private void purgeTicket(Long ticketId) {
        attachmentService.deleteAttachmentsForTicket(ticketId);
        ticketHistoryService.deleteByTicketId(ticketId);
        commentRepository.deleteByTicketId(ticketId);
        ticketRepository.deleteById(ticketId);
    }

    private CommentResponse toCommentResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .ticketId(c.getTicket() != null ? c.getTicket().getId() : null)
                .incidentId(c.getIncident() != null ? c.getIncident().getId() : null)
                .authorId(c.getUser() != null ? c.getUser().getId() : null)
                .authorUsername(c.getUser() != null ? c.getUser().getUsername() : null)
                .text(c.getText())
                .timestamp(c.getTimestamp())
                .build();
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .creatorId(t.getCreator() != null ? t.getCreator().getId() : null)
                .creatorUsername(t.getCreator() != null ? t.getCreator().getUsername() : null)
                .assignedUserId(t.getAssignedUser() != null ? t.getAssignedUser().getId() : null)
                .assignedUsername(t.getAssignedUser() != null ? t.getAssignedUser().getUsername() : null)
                .candidateUserIds(t.getCandidates().stream().map(User::getId).toList())
                .candidateUsernamesDisplay(t.getCandidates().isEmpty() ? null :
                        t.getCandidates().stream().map(User::getUsername).collect(java.util.stream.Collectors.joining(", ")))
                .departmentId(t.getDepartment() != null ? t.getDepartment().getId() : null)
                .departmentName(t.getDepartment() != null ? t.getDepartment().getName() : null)
                .departmentLocation(t.getDepartment() != null ? t.getDepartment().getLocation() : null)
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
                .source(t.getSource())
                .incidentId(t.getIncident() != null ? t.getIncident().getId() : null)
                .incidentSubject(t.getIncident() != null ? t.getIncident().getSubject() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }


}
