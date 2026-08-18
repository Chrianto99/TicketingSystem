package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.response.AttachmentCleanupResponse;
import com.Chrianto.TicketingSystem.dto.response.AttachmentDownload;
import com.Chrianto.TicketingSystem.dto.response.AttachmentResponse;
import com.Chrianto.TicketingSystem.entity.Attachment;
import com.Chrianto.TicketingSystem.entity.IncidentReport;
import com.Chrianto.TicketingSystem.entity.Ticket;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.TicketAction;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.AttachmentRepository;
import com.Chrianto.TicketingSystem.repository.IncidentReportRepository;
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
    private final IncidentReportRepository incidentReportRepository;
    private final TicketHistoryService ticketHistoryService;

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
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε ticket με id: " + ticketId));

        Attachment attachment = storeAttachment(file, "ticket-" + ticket.getId(), currentUser);
        attachment.setTicket(ticket);
        attachment = attachmentRepository.save(attachment);

        ticketHistoryService.logHistory(ticket, currentUser, TicketAction.ATTACHMENT_ADDED, null, null, attachment.getFileName());

        return toResponse(attachment);
    }

    public AttachmentResponse uploadAttachmentForIncident(Long incidentId, MultipartFile file, User currentUser) {
        IncidentReport incident = incidentReportRepository.findById(incidentId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε αναφορά συμβάντος με id: " + incidentId));

        Attachment attachment = storeAttachment(file, "incident-" + incident.getId(), currentUser);
        attachment.setIncidentReport(incident);
        attachmentRepository.save(attachment);

        return toResponse(attachment);
    }

    // Shared storage step for both upload paths — writes the file to disk and
    // builds the Attachment shell; the caller sets which parent (Ticket or
    // IncidentReport) it belongs to before saving.
    private Attachment storeAttachment(MultipartFile file, String storageDirName, User currentUser) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Το αρχείο δεν πρέπει να είναι κενό");
        }

        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
                        ? "file"
                        : file.getOriginalFilename());
        String storedName = UUID.randomUUID().toString();
        String relativePath = storageDirName + "/" + storedName;

        try {
            Path targetDir = storageRoot.resolve(storageDirName);
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(storedName));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store attachment", e);
        }

        Attachment attachment = new Attachment();
        attachment.setUploadedBy(currentUser);
        attachment.setFileName(originalName);
        attachment.setFilePath(relativePath);
        attachment.setContentType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setUploadedAt(LocalDateTime.now());
        return attachment;
    }

    public List<AttachmentResponse> getAttachmentsForTicket(Long ticketId) {
        return attachmentRepository.findByTicketId(ticketId).stream()
                .map(AttachmentService::toResponse)
                .toList();
    }

    public List<AttachmentResponse> getAttachmentsForIncident(Long incidentId) {
        return attachmentRepository.findByIncidentReportId(incidentId).stream()
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

        if (attachment.getTicket() != null) {
            ticketHistoryService.logHistory(attachment.getTicket(), currentUser, TicketAction.ATTACHMENT_REMOVED, null, null, attachment.getFileName());
        }
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

    private static final int CLEANUP_MIN_AGE_DAYS = 30;
    private static final List<TicketStatus> CLEANUP_ELIGIBLE_STATUSES = List.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED);

    public AttachmentCleanupResponse previewCleanup(int olderThanDays) {
        List<Attachment> eligible = findCleanupCandidates(olderThanDays);
        return AttachmentCleanupResponse.builder()
                .count(eligible.size())
                .totalBytes(totalBytes(eligible))
                .build();
    }

    // Purges attachments older than the given threshold, but only on tickets that
    // are RESOLVED or CANCELLED — never touches anything still OPEN. Each deletion
    // is logged to its ticket's history like a manual delete, so there's no silent
    // gap in the audit trail months later.
    @Transactional
    public AttachmentCleanupResponse cleanupOldAttachments(int olderThanDays, User performedBy) {
        List<Attachment> eligible = findCleanupCandidates(olderThanDays);
        long freedBytes = totalBytes(eligible);

        for (Attachment attachment : eligible) {
            deleteFileQuietly(attachment);
            ticketHistoryService.logHistory(attachment.getTicket(), performedBy, TicketAction.ATTACHMENT_REMOVED,
                    null, null, attachment.getFileName() + " (αυτόματη εκκαθάριση παλαιών συνημμένων)");
        }
        attachmentRepository.deleteAll(eligible);

        return AttachmentCleanupResponse.builder()
                .count(eligible.size())
                .totalBytes(freedBytes)
                .build();
    }

    private List<Attachment> findCleanupCandidates(int olderThanDays) {
        if (olderThanDays < CLEANUP_MIN_AGE_DAYS) {
            throw new IllegalArgumentException("Η εκκαθάριση απαιτεί τουλάχιστον " + CLEANUP_MIN_AGE_DAYS + " ημέρες παλαιότητας");
        }
        LocalDateTime cutoff = LocalDateTime.now().minusDays(olderThanDays);
        return attachmentRepository.findEligibleForCleanup(cutoff, CLEANUP_ELIGIBLE_STATUSES);
    }

    private long totalBytes(List<Attachment> attachments) {
        return attachments.stream().mapToLong(a -> a.getFileSize() != null ? a.getFileSize() : 0L).sum();
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
