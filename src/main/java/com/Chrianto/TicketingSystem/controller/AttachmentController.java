package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.response.AttachmentDownload;
import com.Chrianto.TicketingSystem.dto.response.AttachmentResponse;
import com.Chrianto.TicketingSystem.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping("/comments/{commentId}/attachments")
    public ResponseEntity<AttachmentResponse> uploadAttachment(@PathVariable Long commentId,
                                                                 @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attachmentService.uploadAttachment(commentId, file));
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
