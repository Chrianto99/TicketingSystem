package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.response.AttachmentDownload;
import com.Chrianto.TicketingSystem.dto.response.AttachmentResponse;
import com.Chrianto.TicketingSystem.entity.Attachment;
import com.Chrianto.TicketingSystem.entity.Comment;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.AttachmentRepository;
import com.Chrianto.TicketingSystem.repository.CommentRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final CommentRepository commentRepository;

    @Value("${app.attachments.dir:uploads}")
    private String uploadDir;

    private Path storageRoot;

    @PostConstruct
    void init() {
        storageRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create attachment storage directory", e);
        }
    }

    public AttachmentResponse uploadAttachment(Long commentId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found with id: " + commentId));

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                        ? "file"
                        : file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString();
        String relativePath = comment.getId() + "/" + storedName;

        try {
            Path targetDir = storageRoot.resolve(String.valueOf(comment.getId()));
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(storedName));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store attachment", e);
        }

        Attachment attachment = new Attachment();
        attachment.setComment(comment);
        attachment.setFileName(originalName);
        attachment.setFilePath(relativePath);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setUploadedAt(LocalDateTime.now());
        attachment = attachmentRepository.save(attachment);

        return toResponse(attachment);
    }

    public AttachmentDownload downloadAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Attachment not found with id: " + attachmentId));

        Path filePath = storageRoot.resolve(attachment.getFilePath()).normalize();
        if (!filePath.startsWith(storageRoot)) {
            throw new EntityNotFoundException("Attachment not found with id: " + attachmentId);
        }

        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            throw new EntityNotFoundException("Attachment file is missing from storage");
        }

        return AttachmentDownload.builder()
                .resource(resource)
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .build();
    }

    @Transactional
    public void deleteAttachmentsForTicket(Long ticketId) {
        List<Attachment> attachments = attachmentRepository.findByComment_Ticket_Id(ticketId);
        attachments.forEach(this::deleteFileQuietly);
        attachmentRepository.deleteAll(attachments);
    }

    private void deleteFileQuietly(Attachment attachment) {
        try {
            Files.deleteIfExists(storageRoot.resolve(attachment.getFilePath()).normalize());
        } catch (IOException ignored) {
            // best-effort cleanup — the DB row removal is what matters for consistency
        }
    }

    public static AttachmentResponse toResponse(Attachment a) {
        return AttachmentResponse.builder()
                .id(a.getId())
                .fileName(a.getFileName())
                .contentType(a.getContentType())
                .fileSize(a.getFileSize())
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
