package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByTicketId(Long ticketId);

    @Modifying
    @Query("UPDATE Attachment a SET a.uploadedBy = null WHERE a.uploadedBy.id = :userId")
    void nullifyUploadedBy(@Param("userId") Long userId);
}
