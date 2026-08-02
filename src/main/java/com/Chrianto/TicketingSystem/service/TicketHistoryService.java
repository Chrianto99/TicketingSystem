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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketHistoryService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;

    public void deleteByTicketId(Long ticketId) {
        ticketHistoryRepository.deleteByTicketId(ticketId);
    }

    public List<TicketHistoryResponse> getTicketHistory(Long ticketId) {
        // confirm the ticket actually exists before querying its history
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        return ticketHistoryRepository.findByTicketIdOrderByTimestampAsc(ticketId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }


    public void logHistory(Ticket ticket, User performedBy, TicketAction ticketAction,
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
                    throw new IllegalArgumentException(ticketAction + ": απαιτείται ανάδοχος χρήστης");
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
                    throw new IllegalArgumentException("Η ακύρωση απαιτεί σχόλιο");
                }
                h.setComment(comment);
            }
            case COMMENT_ADDED, REOPENED -> {
                if (comment == null) {
                    throw new IllegalArgumentException("Δεν έχει πληκτρολογηθεί σχόλιο");
                }
                h.setComment(comment);
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
                .attachments(h.getComment() != null
                        ? h.getComment().getAttachments().stream().map(AttachmentService::toResponse).collect(Collectors.toList())
                        : List.of())
                .timestamp(h.getTimestamp())
                .build();
    }
}
