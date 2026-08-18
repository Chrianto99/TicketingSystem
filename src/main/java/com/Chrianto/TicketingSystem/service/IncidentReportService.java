package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.IncidentCommentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentReportCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentReportUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentTicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.CommentResponse;
import com.Chrianto.TicketingSystem.dto.response.IncidentReportResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.Comment;
import com.Chrianto.TicketingSystem.entity.IncidentReport;
import com.Chrianto.TicketingSystem.entity.Ticket;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketSource;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.CommentRepository;
import com.Chrianto.TicketingSystem.repository.IncidentReportRepository;
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
public class IncidentReportService {

    private final IncidentReportRepository incidentReportRepository;
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TicketHistoryService ticketHistoryService;

    public IncidentReportResponse createIncidentReport(IncidentReportCreateRequest req, User creator) {
        IncidentReport report = new IncidentReport();
        report.setCreator(creator);
        report.setSubject(req.getSubject());
        report.setDescription(req.getDescription());
        report.setPriority(req.getPriority());
        report.setStatus(IncidentStatus.OPEN);
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());

        report = incidentReportRepository.save(report);
        return toResponse(report);
    }

    public IncidentReportResponse getIncidentReportById(Long incidentId) {
        IncidentReport incident = incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));
        return toResponse(incident);
    }

    public Page<IncidentReportResponse> getAllIncidentReports(IncidentStatus status, Pageable pageable) {
        Page<IncidentReport> page = status != null
                ? incidentReportRepository.findByStatus(status, pageable)
                : incidentReportRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    // Full replace, same convention as TicketService.editTicket() — both fields
    // are resent together rather than patched individually.
    public IncidentReportResponse editIncidentReport(Long incidentId, IncidentReportUpdateRequest req) {
        IncidentReport incident = incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        incident.setSubject(req.getSubject());
        incident.setDescription(req.getDescription());
        incident.setPriority(req.getPriority());
        incident.setUpdatedAt(LocalDateTime.now());

        incident = incidentReportRepository.save(incident);
        return toResponse(incident);
    }

    public IncidentReportResponse closeIncident(Long incidentId) {
        IncidentReport incident = incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        if (incident.getStatus() == IncidentStatus.CLOSED) {
            throw new IllegalStateException("Η αναφορά συμβάντος είναι ήδη " + incident.getStatus().getDisplayName());
        }

        incident.setStatus(IncidentStatus.CLOSED);
        incident.setUpdatedAt(LocalDateTime.now());
        incident = incidentReportRepository.save(incident);
        return toResponse(incident);
    }

    public IncidentReportResponse reopenIncident(Long incidentId) {
        IncidentReport incident = incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        if (incident.getStatus() == IncidentStatus.OPEN) {
            throw new IllegalStateException("Η αναφορά συμβάντος είναι ήδη " + incident.getStatus().getDisplayName());
        }

        incident.setStatus(IncidentStatus.OPEN);
        incident.setUpdatedAt(LocalDateTime.now());
        incident = incidentReportRepository.save(incident);
        return toResponse(incident);
    }

    public CommentResponse addComment(Long incidentId, IncidentCommentCreateRequest req, User author) {
        IncidentReport incident = incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        Comment comment = new Comment();
        comment.setIncidentReport(incident);
        comment.setUser(author);
        comment.setText(req.getCommentText());
        comment.setTimestamp(LocalDateTime.now());

        comment = commentRepository.save(comment);
        return toCommentResponse(comment);
    }

    public List<CommentResponse> getCommentsForIncident(Long incidentId) {
        incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));
        return commentRepository.findByIncidentReportIdOrderByTimestampAscIdAsc(incidentId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    // Spawns a Ticket from an IncidentReport: TicketSource.INCIDENT, linked back
    // via Ticket.incidentReport, always created OPEN — resolution happens
    // through the normal ticket resolve flow afterward, not at creation.
    @Transactional
    public TicketResponse createTicketFromIncident(Long incidentId, IncidentTicketCreateRequest req, User creator) {
        IncidentReport incident = incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        User assignee = userRepository.findById(req.getAssignedUserId())
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + req.getAssignedUserId()));

        Ticket ticket = new Ticket();
        ticket.setIncidentReport(incident);
        ticket.setCreator(creator);
        ticket.setPriority(incident.getPriority());
        ticket.setAssignedUser(assignee);
        ticket.setSummary(req.getTitle());
        ticket.setDescription(req.getDescription());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setSource(TicketSource.INCIDENT);
        ticket.setCreatedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        ticket = ticketRepository.save(ticket);

        ticketHistoryService.logHistory(ticket, creator, TicketAction.CREATED, null, null, null);
        ticketHistoryService.logHistory(ticket, creator, TicketAction.ASSIGNED, assignee, null, null);

        return toTicketResponse(ticket);
    }

    public List<TicketResponse> getTicketsForIncident(Long incidentId) {
        incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));
        return ticketRepository.findByIncidentReportIdOrderByCreatedAtDesc(incidentId).stream()
                .map(this::toTicketResponse)
                .toList();
    }

    private IncidentReportResponse toResponse(IncidentReport report) {
        return IncidentReportResponse.builder()
                .id(report.getId())
                .creatorId(report.getCreator() != null ? report.getCreator().getId() : null)
                .creatorUsername(report.getCreator() != null ? report.getCreator().getUsername() : null)
                .subject(report.getSubject())
                .description(report.getDescription())
                .status(report.getStatus())
                .priority(report.getPriority())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private CommentResponse toCommentResponse(Comment c) {
        return CommentResponse.builder()
                .id(c.getId())
                .incidentReportId(c.getIncidentReport() != null ? c.getIncidentReport().getId() : null)
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
                .incidentReportId(t.getIncidentReport() != null ? t.getIncidentReport().getId() : null)
                .incidentSubject(t.getIncidentReport() != null ? t.getIncidentReport().getSubject() : null)
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .build();
    }
}
