package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketIdOrderByTimestampAscIdAsc(Long ticketId);

    void deleteByTicketId(Long ticketId);

    // Statistics: RESOLVED/CANCELLED events per day/month — counts the event, not
    // "currently resolved tickets", so a reopened-then-resolved-again ticket counts
    // twice. "timestamp" is quoted because it's a reserved word in Postgres.
    @Query(value = "SELECT date_trunc('day', \"timestamp\") AS period, COUNT(*) " +
            "FROM ticket_history WHERE action = :action AND \"timestamp\" >= :from GROUP BY period ORDER BY period",
            nativeQuery = true)
    List<Object[]> countActionByDay(@Param("action") String action, @Param("from") LocalDateTime from);

    @Query(value = "SELECT date_trunc('month', \"timestamp\") AS period, COUNT(*) " +
            "FROM ticket_history WHERE action = :action AND \"timestamp\" >= :from GROUP BY period ORDER BY period",
            nativeQuery = true)
    List<Object[]> countActionByMonth(@Param("action") String action, @Param("from") LocalDateTime from);

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
