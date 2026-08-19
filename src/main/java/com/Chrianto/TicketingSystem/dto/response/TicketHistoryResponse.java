package com.Chrianto.TicketingSystem.dto.response;

import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
public class TicketHistoryResponse {
    private Long id;
    private TicketAction action;
    private Long performedById;
    private String performedByUsername;
    private String description;
    private LocalDateTime timestamp;
}
