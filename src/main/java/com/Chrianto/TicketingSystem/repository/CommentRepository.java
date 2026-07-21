package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    void deleteByTicketId(Long ticketId);

    @Modifying
    @Query("UPDATE Comment c SET c.user = null WHERE c.user.id = :userId")
    void nullifyAuthor(@Param("userId") Long userId);
}
