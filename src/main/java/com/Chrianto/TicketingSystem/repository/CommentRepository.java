package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // Newest first, matching how the comments tab has always displayed them.
    List<Comment> findByTicketIdOrderByTimestampAscIdAsc(Long ticketId);

    // Oldest first — the incident detail page reads like a chat log, newest at the bottom.
    List<Comment> findByIncidentIdOrderByTimestampAscIdAsc(Long incidentId);

    void deleteByTicketId(Long ticketId);

    @Modifying
    @Query("UPDATE Comment c SET c.user = null WHERE c.user.id = :userId")
    void nullifyAuthor(@Param("userId") Long userId);
}
