package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.response.AttachmentCleanupResponse;
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
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PostMapping("/incident-reports/{incidentId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadAttachmentForIncident(@PathVariable Long incidentId,
                                                                            @RequestParam("file") MultipartFile file,
                                                                            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.uploadAttachmentForIncident(incidentId, file, currentUser));
    }

    @GetMapping("/incident-reports/{incidentId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> getAttachmentsForIncident(@PathVariable Long incidentId) {
        return ResponseEntity.ok(attachmentService.getAttachmentsForIncident(incidentId));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId,
                                                  @AuthenticationPrincipal User currentUser) {
        attachmentService.deleteAttachment(attachmentId, currentUser);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/attachments/cleanup/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttachmentCleanupResponse> previewCleanup(@RequestParam int olderThanDays) {
        return ResponseEntity.ok(attachmentService.previewCleanup(olderThanDays));
    }

    @PostMapping("/attachments/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AttachmentCleanupResponse> cleanup(@RequestParam int olderThanDays,
                                                               @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(attachmentService.cleanupOldAttachments(olderThanDays, currentUser));
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
