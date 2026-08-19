package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.response.IncidentHistoryResponse;
import com.Chrianto.TicketingSystem.dto.response.TicketHistoryResponse;
import com.Chrianto.TicketingSystem.entity.*;
import com.Chrianto.TicketingSystem.entity.enums.IncidentAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.IncidentRepository;
import com.Chrianto.TicketingSystem.repository.TicketHistoryRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketHistoryService {

    private static final int DESCRIPTION_MAX_LENGTH = 1997;

    private final TicketRepository ticketRepository;
    private final IncidentRepository incidentRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    public void deleteByTicketId(Long ticketId) {
        ticketHistoryRepository.deleteByTicketId(ticketId);
    }

    public List<TicketHistoryResponse> getTicketHistory(Long ticketId) {
        // confirm the ticket actually exists before querying its history
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        return ticketHistoryRepository.findByTicketIdOrderByTimestampAscIdAsc(ticketId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    public List<IncidentHistoryResponse> getIncidentHistory(Long incidentId) {
        incidentRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        return ticketHistoryRepository.findByIncidentIdOrderByTimestampAscIdAsc(incidentId)
                .stream()
                .map(this::toIncidentHistoryResponse)
                .toList();
    }

    // No assignedTo column exists anymore: the assignee is folded into the
    // composed sentence in `description` at write time instead, so old and new
    // entries render identically once V11's backfill matches this format.
    public void logHistory(Ticket ticket, User performedBy, TicketAction ticketAction,
                            User assignedTo, Comment comment, String description) {

        TicketHistory h = new TicketHistory();
        h.setTicket(ticket);
        h.setPerformedBy(performedBy);
        h.setTicketAction(ticketAction);
        h.setTimestamp(LocalDateTime.now());
        h.setDescription(description);

        switch (ticketAction) {
            case CREATED -> {
                // nothing extra to attach
            }
            case ASSIGNED, REASSIGNED -> {
                if (assignedTo == null) {
                    throw new IllegalArgumentException(ticketAction + ": απαιτείται ανάδοχος χρήστης");
                }
                String verb = ticketAction == TicketAction.ASSIGNED ? "ανέθεσε" : "επανέθεσε";
                String sentence = performedBy.getUsername() + " " + verb + " το ticket σε " + assignedTo.getUsername() + ".";
                if (description != null && !description.isBlank()) {
                    sentence += " Λόγος: " + description;
                }
                h.setDescription(sentence);
            }
            case RESOLVED -> {
                // nothing extra to attach
            }
            case CANCELLED, REOPENED -> {
                if (description == null || description.isBlank()) {
                    throw new IllegalArgumentException(ticketAction + ": απαιτείται λόγος");
                }
            }
            case COMMENT_ADDED -> {
                if (comment == null) {
                    throw new IllegalArgumentException("Δεν έχει πληκτρολογηθεί σχόλιο");
                }
                String commentText = comment.getText();
                h.setDescription(commentText.length() > DESCRIPTION_MAX_LENGTH
                        ? commentText.substring(0, DESCRIPTION_MAX_LENGTH) + "…"
                        : commentText);
            }
            case COMMENT_EDITED, INFO_CHANGED, COMMENT_REMOVED -> {
                // description carries the change summary / removed text
            }
            case ATTACHMENT_ADDED, ATTACHMENT_REMOVED -> {
                // description carries the file name
            }

            default -> throw new IllegalArgumentException("Μη υποστηριζόμενη ενέργεια ticket: " + ticketAction);
        }

        ticketHistoryRepository.save(h);
    }

    // Mirrors logHistory() above for the Incident side of the same shared table.
    // `description` must already be the full display sentence for every action
    // except COMMENT_ADDED, which derives its text from `comment` instead.
    public void logIncidentHistory(Incident incident, User performedBy, IncidentAction incidentAction,
                                    Comment comment, String description) {

        TicketHistory h = new TicketHistory();
        h.setIncident(incident);
        h.setPerformedBy(performedBy);
        h.setIncidentAction(incidentAction);
        h.setTimestamp(LocalDateTime.now());
        h.setDescription(description);

        switch (incidentAction) {
            case COMMENT_ADDED -> {
                if (comment == null) {
                    throw new IllegalArgumentException("Δεν έχει πληκτρολογηθεί σχόλιο");
                }
                String commentText = comment.getText();
                h.setDescription(commentText.length() > DESCRIPTION_MAX_LENGTH
                        ? commentText.substring(0, DESCRIPTION_MAX_LENGTH) + "…"
                        : commentText);
            }
            case REPORTED, CLOSED, REOPENED, INFO_CHANGED, COMMENT_EDITED, COMMENT_REMOVED,
                 ATTACHMENT_ADDED, ATTACHMENT_REMOVED,
                 TICKET_ASSIGNED, TICKET_RESOLVED, TICKET_CANCELLED, TICKET_REOPENED -> {
                if (description == null || description.isBlank()) {
                    throw new IllegalArgumentException(incidentAction + ": απαιτείται περιγραφή");
                }
            }
            default -> throw new IllegalArgumentException("Μη υποστηριζόμενη ενέργεια συμβάντος: " + incidentAction);
        }

        ticketHistoryRepository.save(h);
    }

    private TicketHistoryResponse toHistoryResponse(TicketHistory h) {
        return TicketHistoryResponse.builder()
                .id(h.getId())
                .action(h.getTicketAction())
                .performedById(h.getPerformedBy() != null ? h.getPerformedBy().getId() : null)
                .performedByUsername(h.getPerformedBy() != null ? h.getPerformedBy().getUsername() : null)
                .description(h.getDescription())
                .timestamp(h.getTimestamp())
                .build();
    }

    private static final String ICON_PLUS = "+";
    private static final String ICON_CHECK = "✓";
    private static final String ICON_CROSS = "✗";
    private static final String ICON_UNDO = "↺";
    private static final String ICON_EDIT = "✎";
    // Every cross-posted ticket-lifecycle event on an incident's timeline shares
    // this same "ticket" glyph — only the CSS class (icon-assigned/resolved/
    // cancelled/reopened) differs, so they're told apart by colour, not shape.
    private static final String ICON_TICKET = "🎫";

    private IncidentHistoryResponse toIncidentHistoryResponse(TicketHistory h) {
        String performer = h.getPerformedBy() != null ? h.getPerformedBy().getUsername() : "Διαγραμμένος χρήστης";
        IncidentAction action = h.getIncidentAction();

        String icon;
        String cssClass;
        String mainText;
        String quote = null;

        switch (action) {
            case REPORTED -> {
                icon = ICON_PLUS; cssClass = "icon-created";
                mainText = h.getDescription();
            }
            case CLOSED -> {
                icon = ICON_CROSS; cssClass = "icon-cancelled";
                mainText = h.getDescription();
            }
            case REOPENED -> {
                icon = ICON_UNDO; cssClass = "icon-reopened";
                mainText = h.getDescription();
            }
            case TICKET_ASSIGNED -> {
                icon = ICON_TICKET; cssClass = "icon-assigned";
                mainText = h.getDescription();
            }
            case TICKET_RESOLVED -> {
                icon = ICON_TICKET; cssClass = "icon-resolved";
                mainText = h.getDescription();
            }
            case TICKET_CANCELLED -> {
                icon = ICON_TICKET; cssClass = "icon-cancelled";
                mainText = h.getDescription();
            }
            case TICKET_REOPENED -> {
                icon = ICON_TICKET; cssClass = "icon-reopened";
                mainText = h.getDescription();
            }
            case COMMENT_REMOVED -> {
                icon = ICON_CROSS; cssClass = "icon-cancelled";
                mainText = performer + " διέγραψε μια αναφορά.";
                quote = h.getDescription();
            }
            case ATTACHMENT_REMOVED -> {
                icon = ICON_CROSS; cssClass = "icon-cancelled";
                mainText = performer + " αφαίρεσε ένα αρχείο:";
                quote = h.getDescription();
            }
            case COMMENT_ADDED -> {
                icon = ICON_EDIT; cssClass = "icon-edited";
                mainText = "Ο χρήστης " + performer + " ανέφερε:";
                quote = h.getDescription();
            }
            case ATTACHMENT_ADDED -> {
                icon = ICON_EDIT; cssClass = "icon-edited";
                mainText = performer + " επισύναψε ένα αρχείο:";
                quote = h.getDescription();
            }
            case INFO_CHANGED -> {
                icon = ICON_EDIT; cssClass = "icon-edited";
                mainText = h.getDescription();
            }
            case COMMENT_EDITED -> {
                icon = ICON_EDIT; cssClass = "icon-edited";
                mainText = performer + " επεξεργάστηκε μια αναφορά.";
                quote = h.getDescription();
            }
            default -> {
                icon = "•"; cssClass = "icon-default";
                mainText = h.getDescription();
            }
        }

        return IncidentHistoryResponse.builder()
                .id(h.getId())
                .action(action)
                .icon(icon)
                .iconCssClass(cssClass)
                .performedByUsername(performer)
                .mainText(mainText)
                .quote(quote)
                .timestamp(h.getTimestamp())
                .build();
    }
}
