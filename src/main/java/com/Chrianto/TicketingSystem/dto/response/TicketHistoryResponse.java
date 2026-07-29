package com.Chrianto.TicketingSystem.dto.response;

import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder
public class TicketHistoryResponse {
    private Long id;
    private TicketAction action;
    private Long performedById;
    private String performedByUsername;
    private Long assignedToId;
    private String assignedToUsername;
    private Long commentId;
    private String commentText;
    private List<AttachmentResponse> attachments;
    private LocalDateTime timestamp;
}