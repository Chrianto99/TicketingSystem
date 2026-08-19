package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Attachment;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByTicketId(Long ticketId);

    List<Attachment> findByIncidentId(Long incidentId);

    @Modifying
    @Query("UPDATE Attachment a SET a.uploadedBy = null WHERE a.uploadedBy.id = :userId")
    void nullifyUploadedBy(@Param("userId") Long userId);

    // Cleanup candidates: never touches attachments on a ticket that's still OPEN,
    // regardless of age.
    @Query("SELECT a FROM Attachment a WHERE a.uploadedAt < :cutoff AND a.ticket.status IN :statuses")
    List<Attachment> findEligibleForCleanup(@Param("cutoff") LocalDateTime cutoff,
                                             @Param("statuses") List<TicketStatus> statuses);
}
