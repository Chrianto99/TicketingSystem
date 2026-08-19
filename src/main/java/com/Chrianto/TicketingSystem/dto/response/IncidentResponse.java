package com.Chrianto.TicketingSystem.dto.response;

import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter @Builder
public class IncidentResponse {
    private Long id;
    private Long creatorId;
    private String creatorUsername;
    private String subject;
    private String description;
    private IncidentStatus status;
    private TicketPriority priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
