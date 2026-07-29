package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;
import org.springframework.core.io.Resource;

// Service-to-controller transfer object for streaming a file back — not serialized as JSON.
@Getter @Builder
public class AttachmentDownload {
    private final Resource resource;
    private final String fileName;
    private final String contentType;
}
