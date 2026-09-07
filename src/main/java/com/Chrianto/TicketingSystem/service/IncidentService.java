package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.CommentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentCommentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentTicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.CommentResponse;
import com.Chrianto.TicketingSystem.dto.response.IncidentResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.Comment;
import com.Chrianto.TicketingSystem.entity.Incident;
import com.Chrianto.TicketingSystem.entity.Ticket;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketSource;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import static com.Chrianto.TicketingSystem.entity.enums.IncidentAction.*;
import com.Chrianto.TicketingSystem.repository.CommentRepository;
import com.Chrianto.TicketingSystem.repository.IncidentRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
import com.Chrianto.TicketingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;
    private final NotificationService notificationService;

    public IncidentResponse createIncident(IncidentCreateRequest req, User creator) {
        Incident incident = new Incident();
        incident.setCreator(creator);
        incident.setSubject(req.getSubject());
        incident.setDescription(req.getDescription());
        incident.setPriority(req.getPriority());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedAt(LocalDateTime.now());
        incident.setUpdatedAt(LocalDateTime.now());

        incident = incidentRepository.save(incident);
        ticketHistoryService.logIncidentHistory(incident, creator, REPORTED, null,
                "Ο χρήστης " + creator.getUsername() + " ανέφερε το συμβάν.");
        return toResponse(incident);
    }

    public IncidentResponse getIncidentById(Long incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));
        return toResponse(incident);
    }

    public Page<IncidentResponse> getAllIncidents(IncidentStatus status, TicketPriority priority,
                                                   String query, Pageable pageable) {
        return incidentRepository.search(status, priority, query, pageable)
                .map(this::toResponse);
    }

    // Full replace, same convention as TicketService.editTicket() — both fields
    // are resent together rather than patched individually.
    public IncidentResponse editIncident(Long incidentId, IncidentUpdateRequest req, User performedBy) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        incident.setSubject(req.getSubject());
        incident.setDescription(req.getDescription());
        incident.setPriority(req.getPriority());
        incident.setUpdatedAt(LocalDateTime.now());

        incident = incidentRepository.save(incident);
        ticketHistoryService.logIncidentHistory(incident, performedBy, INFO_CHANGED, null,
                "Ο χρήστης " + performedBy.getUsername() + " επεξεργάστηκε τις πληροφορίες του συμβάντος.");
        return toResponse(incident);
    }

    // Closing/reopening now happens through a report rather than a bare status
    // flip: the report's text becomes both the Comment shown in the Αναφορές
    // thread and the quote on this event's History entry. An incident can
    // cycle open/closed many times, so every cycle's explanation stays visible
    // in the thread in order — unlike a single "resolution" column, which only
    // ever held the latest one.
    @Transactional
    public IncidentResponse closeIncident(Long incidentId, IncidentCommentCreateRequest req, User performedBy) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new IllegalStateException("Η αναφορά συμβάντος είναι ήδη " + incident.getStatus().getDisplayName());
        }

        postComment(incident, performedBy, req.getCommentText());

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setUpdatedAt(LocalDateTime.now());
        incident = incidentRepository.save(incident);
        ticketHistoryService.logIncidentHistory(incident, performedBy, CLOSED, null, req.getCommentText());
        return toResponse(incident);
    }

    @Transactional
    public IncidentResponse reopenIncident(Long incidentId, IncidentCommentCreateRequest req, User performedBy) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        if (incident.getStatus() == IncidentStatus.OPEN) {
            throw new IllegalStateException("Η αναφορά συμβάντος είναι ήδη " + incident.getStatus().getDisplayName());
        }

        postComment(incident, performedBy, req.getCommentText());

        incident.setStatus(IncidentStatus.OPEN);
        incident.setUpdatedAt(LocalDateTime.now());
        incident = incidentRepository.save(incident);
        ticketHistoryService.logIncidentHistory(incident, performedBy, REOPENED, null, req.getCommentText());
        return toResponse(incident);
    }

    private void postComment(Incident incident, User author, String text) {
        Comment comment = new Comment();
        comment.setIncident(incident);
        comment.setUser(author);
        comment.setText(text);
        comment.setTimestamp(LocalDateTime.now());
        commentRepository.save(comment);
    }

    public CommentResponse addComment(Long incidentId, IncidentCommentCreateRequest req, User author) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        Comment comment = new Comment();
        comment.setIncident(incident);
        comment.setUser(author);
        comment.setText(req.getCommentText());
        comment.setTimestamp(LocalDateTime.now());
        comment = commentRepository.save(comment);

        ticketHistoryService.logIncidentHistory(incident, author, COMMENT_ADDED, comment, null);
        return toCommentResponse(comment);
    }

    private static final int HISTORY_COMMENT_DIFF_MAX_LENGTH = 900;

    // Only the original author may edit/delete their own report — no ADMIN
    // override, matching TicketService's editComment/deleteComment.
    public void editComment(Long commentId, CommentUpdateRequest req, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Η αναφορά δεν βρέθηκε"));

        if (comment.getUser() == null || !comment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Μόνο ο χρήστης που την ανέφερε μπορεί να επεξεργαστεί αυτή την αναφορά");
        }

        String oldText = comment.getText();
        comment.setText(req.getText());
        commentRepository.save(comment);

        if (!oldText.trim().equals(req.getText().trim())) {
            String description = "Αναφορά: «" + truncateForHistory(oldText.trim()) +
                    "» → «" + truncateForHistory(req.getText().trim()) + "»";
            ticketHistoryService.logIncidentHistory(comment.getIncident(), currentUser, COMMENT_EDITED, null, description);
        }
    }

    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Η αναφορά δεν βρέθηκε"));

        if (comment.getUser() == null || !comment.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Μόνο ο χρήστης που την ανέφερε μπορεί να διαγράψει αυτή την αναφορά");
        }

        Incident incident = comment.getIncident();
        String commentText = comment.getText();

        commentRepository.delete(comment);

        ticketHistoryService.logIncidentHistory(incident, currentUser, COMMENT_REMOVED, null,
                truncateForHistory(commentText.trim()));
    }

    private String truncateForHistory(String value) {
        return value.length() > HISTORY_COMMENT_DIFF_MAX_LENGTH
                ? value.substring(0, HISTORY_COMMENT_DIFF_MAX_LENGTH) + "…"
                : value;
    }

    public List<CommentResponse> getCommentsForIncident(Long incidentId) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));
        return commentRepository.findByIncidentIdOrderByTimestampAscIdAsc(incidentId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    // Spawns a Ticket from an Incident: TicketSource.INCIDENT, linked back
    // via Ticket.incident, always created OPEN — resolution happens
    // through the normal ticket resolve flow afterward, not at creation.
    @Transactional
    public TicketResponse createTicketFromIncident(Long incidentId, IncidentTicketCreateRequest req, User creator) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

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
        ticket.setIncident(incident);
        ticket.setCreator(creator);
        ticket.setPriority(incident.getPriority());
        if (selfClaim) {
            ticket.setAssignedUser(creator);
        } else {
            ticket.getCandidates().addAll(candidates);
        }
        ticket.setSummary(req.getTitle());
        ticket.setDescription(req.getDescription());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setSource(TicketSource.INCIDENT);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, creator, TicketAction.CREATED, null, null, null);

        if (selfClaim) {
            ticketHistoryService.logHistory(ticket, creator, TicketAction.CLAIMED, null, null, null);
            ticketHistoryService.logIncidentHistory(incident, creator, TICKET_ASSIGNED, null,
                    "Ο χρήστης " + creator.getUsername() + " ανέλαβε το ticket #" + ticket.getId() + " («" + ticket.getSummary() + "»).");
            return toTicketResponse(ticket);
        }

        String names = candidates.stream().map(User::getUsername).collect(java.util.stream.Collectors.joining(", "));
        String sentence = "Ο χρήστης " + creator.getUsername() + " πρόσφερε το ticket σε: " + names + ".";
        ticketHistoryService.logHistory(ticket, creator, TicketAction.OFFERED, null, null, sentence);

        // Creation + initial offer happen in the same step for an
        // incident-derived ticket, so they collapse into one TICKET_ASSIGNED
        // entry on the parent incident's timeline rather than a separate
        // "created" entry.
        ticketHistoryService.logIncidentHistory(incident, creator, TICKET_ASSIGNED, null,
                "Ο χρήστης " + creator.getUsername() + " πρόσφερε το ticket #" + ticket.getId() + " («" + ticket.getSummary() + "») σε: " + names + ".");

        for (User candidate : candidates) {
            if (!candidate.getId().equals(creator.getId())) {
                notificationService.notifyTicketAssigned(candidate.getId());
            }
        }

        return toTicketResponse(ticket);
    }

    public List<TicketResponse> getTicketsForIncident(Long incidentId) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));
        return ticketRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
                .map(this::toTicketResponse)
                .toList();
    }

    private IncidentResponse toResponse(Incident incident) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .creatorId(incident.getCreator() != null ? incident.getCreator().getId() : null)
                .creatorUsername(incident.getCreator() != null ? incident.getCreator().getUsername() : null)
                .subject(incident.getSubject())
                .description(incident.getDescription())
                .status(incident.getStatus())
                .priority(incident.getPriority())
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }

    private CommentResponse toCommentResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .incidentId(c.getIncident() != null ? c.getIncident().getId() : null)
                .authorId(c.getUser() != null ? c.getUser().getId() : null)
                .authorUsername(c.getUser() != null ? c.getUser().getUsername() : null)
                .text(c.getText())
                .timestamp(c.getTimestamp())
                .build();
    }

    private TicketResponse toTicketResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .creatorId(t.getCreator() != null ? t.getCreator().getId() : null)
                .creatorUsername(t.getCreator() != null ? t.getCreator().getUsername() : null)
                .assignedUserId(t.getAssignedUser() != null ? t.getAssignedUser().getId() : null)
                .assignedUsername(t.getAssignedUser() != null ? t.getAssignedUser().getUsername() : null)
                .status(t.getStatus())
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
