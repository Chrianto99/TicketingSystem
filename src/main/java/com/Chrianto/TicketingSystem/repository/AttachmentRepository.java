package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByComment_Ticket_Id(Long ticketId);
}
