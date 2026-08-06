package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.response.AttachmentDownload;
import com.Chrianto.TicketingSystem.dto.response.AttachmentResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/tickets/{ticketId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadAttachment(@PathVariable Long ticketId,
                                                                 @RequestParam("file") MultipartFile file,
                                                                 @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.uploadAttachment(ticketId, file, currentUser));
    }

    @GetMapping("/tickets/{ticketId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsForTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsForTicket(ticketId));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId,
                                                  @AuthenticationPrincipal User currentUser) {
        attachmentService.deleteAttachment(attachmentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        AttachmentDownload download = attachmentService.downloadAttachment(attachmentId);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.getFileName(), StandardCharsets.UTF_8)
                .build();

        MediaType mediaType = download.getContentType() != null
                ? MediaType.parseMediaType(download.getContentType())
                : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.getResource());
    }
}
