package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @Builder
public class AttachmentResponse {
    private Long id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private Long uploadedById;
    private LocalDateTime uploadedAt;
}
