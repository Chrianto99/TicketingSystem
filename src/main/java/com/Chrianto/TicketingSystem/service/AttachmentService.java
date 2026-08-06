package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.response.AttachmentDownload;
import com.Chrianto.TicketingSystem.dto.response.AttachmentResponse;
import com.Chrianto.TicketingSystem.entity.Attachment;
import com.Chrianto.TicketingSystem.entity.Ticket;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.AttachmentRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
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
    private final TicketRepository ticketRepository;

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

    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Το αρχείο δεν πρέπει να είναι κενό");
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                        ? "file"
                        : file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString();
        String storageDirName = "ticket-" + ticket.getId();
        String relativePath = storageDirName + "/" + storedName;

        try {
            Path targetDir = storageRoot.resolve(storageDirName);
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(storedName));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store attachment", e);
        }

        Attachment attachment = new Attachment();
        attachment.setTicket(ticket);
        attachment.setUploadedBy(currentUser);
        attachment.setFileName(originalName);
        attachment.setFilePath(relativePath);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setUploadedAt(LocalDateTime.now());
        attachment = attachmentRepository.save(attachment);

        return toResponse(attachment);
    }

    public List<AttachmentResponse> getAttachmentsForTicket(Long ticketId) {
        return attachmentRepository.findByTicketId(ticketId).stream()
                .map(AttachmentService::toResponse)
                .toList();
    }

    public void deleteAttachment(Long attachmentId, User currentUser) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε συνημμένο με id: " + attachmentId));

        if (attachment.getUploadedBy() == null || !attachment.getUploadedBy().getId().equals(currentUser.getId())) {
            throw new IllegalStateException("Μόνο ο χρήστης που επισύναψε αυτό το αρχείο μπορεί να το αφαιρέσει");
        }

        deleteFileQuietly(attachment);
        attachmentRepository.delete(attachment);
    }

    public AttachmentDownload downloadAttachment(Long attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε συνημμένο με id: " + attachmentId));

        Path filePath = storageRoot.resolve(attachment.getFilePath()).normalize();
        if (!filePath.startsWith(storageRoot)) {
            throw new EntityNotFoundException("Δεν βρέθηκε συνημμένο με id: " + attachmentId);
        }

        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists()) {
            throw new EntityNotFoundException("Το αρχείο του συνημμένου λείπει από την αποθήκευση");
        }

        return AttachmentDownload.builder()
                .resource(resource)
                .fileName(attachment.getFileName())
                .contentType(attachment.getContentType())
                .build();
    }

    @Transactional
    public void deleteAttachmentsForTicket(Long ticketId) {
        List<Attachment> attachments = attachmentRepository.findByTicketId(ticketId);
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
                .uploadedById(a.getUploadedBy() != null ? a.getUploadedBy().getId() : null)
                .uploadedAt(a.getUploadedAt())
                .build();
    }
}
