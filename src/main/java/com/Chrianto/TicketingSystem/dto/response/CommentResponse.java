package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
public class CommentResponse {
    private Long id;
    private Long ticketId;
    private Long incidentReportId;
    private Long authorId;
    private String authorUsername;
    private String text;
    private LocalDateTime timestamp;
}
