package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.response.TicketHistoryResponse;
import com.Chrianto.TicketingSystem.entity.Comment;
import com.Chrianto.TicketingSystem.entity.Ticket;
import com.Chrianto.TicketingSystem.entity.TicketHistory;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
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
    private final TicketHistoryRepository ticketHistoryRepository;

    public void deleteByTicketId(Long ticketId) {
        ticketHistoryRepository.deleteByTicketId(ticketId);
    }

    // Detaches the comment_id FK from the COMMENT_ADDED history row before the
    // Comment itself is deleted, since that FK has no ON DELETE clause and would
    // otherwise fail the delete with a constraint violation.
    public void unlinkComment(Long commentId) {
        ticketHistoryRepository.nullifyComment(commentId);
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


    public void logHistory(Ticket ticket, User performedBy, TicketAction ticketAction,
                            User assignedTo, Comment comment, String description) {

        TicketHistory h = new TicketHistory();
        h.setTicket(ticket);
        h.setPerformedBy(performedBy);
        h.setAction(ticketAction);
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
                h.setAssignedTo(assignedTo);
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
                h.setComment(comment);
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

    private TicketHistoryResponse toHistoryResponse(TicketHistory h) {
        return TicketHistoryResponse.builder()
                .id(h.getId())
                .action(h.getAction())
                .performedById(h.getPerformedBy() != null ? h.getPerformedBy().getId() : null)
                .performedByUsername(h.getPerformedBy() != null ? h.getPerformedBy().getUsername() : null)
                .assignedToId(h.getAssignedTo() != null ? h.getAssignedTo().getId() : null)
                .assignedToUsername(h.getAssignedTo() != null ? h.getAssignedTo().getUsername() : null)
                .commentId(h.getComment() != null ? h.getComment().getId() : null)
                .commentText(h.getComment() != null ? h.getComment().getText() : null)
                .description(h.getDescription())
                .timestamp(h.getTimestamp())
                .build();
    }
}
