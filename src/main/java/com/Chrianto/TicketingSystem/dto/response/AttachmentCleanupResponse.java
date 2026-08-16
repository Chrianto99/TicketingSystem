package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;

@Getter @Setter @Builder
public class AttachmentCleanupResponse {
    private long count;
    private long totalBytes;
}
