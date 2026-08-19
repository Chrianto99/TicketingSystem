package com.Chrianto.TicketingSystem.dto.response;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketSource;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
public class TicketResponse {
    private Long id;
    private Long creatorId;
    private String creatorUsername;
    private Long assignedUserId;
    private String assignedUsername;
    private Long departmentId;
    private String departmentName;
    private Long categoryId;
    private String category;
    private Long subcategoryId;
    private String subcategory;
    private TicketStatus status;
    private TicketPriority priority;
    private String callerName;
    private String phoneNumber;
    private String ipAddress;
    private String summary;
    private String description;
    private String resolution;
    private TicketSource source;
    private Long incidentId;
    private String incidentSubject;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
