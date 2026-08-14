package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketIdOrderByTimestampAscIdAsc(Long ticketId);

    void deleteByTicketId(Long ticketId);

    @Modifying
    @Query("UPDATE TicketHistory h SET h.performedBy = null WHERE h.performedBy.id = :userId")
    void nullifyPerformedBy(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE TicketHistory h SET h.assignedTo = null WHERE h.assignedTo.id = :userId")
    void nullifyAssignedTo(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE TicketHistory h SET h.comment = null WHERE h.comment.id = :commentId")
    void nullifyComment(@Param("commentId") Long commentId);
}
