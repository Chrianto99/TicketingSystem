package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Ticket;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketSource;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);

    // Statistics: tickets created per day/month, from a given point forward.
    // Native + date_trunc since this is Postgres-only already (see the Flyway
    // migrations) and JPQL has no portable date-bucketing function.
    @Query(value = "SELECT date_trunc('day', created_at) AS period, COUNT(*) " +
            "FROM ticket WHERE created_at >= :from GROUP BY period ORDER BY period", nativeQuery = true)
    List<Object[]> countCreatedByDay(@Param("from") LocalDateTime from);

    @Query(value = "SELECT date_trunc('month', created_at) AS period, COUNT(*) " +
            "FROM ticket WHERE created_at >= :from GROUP BY period ORDER BY period", nativeQuery = true)
    List<Object[]> countCreatedByMonth(@Param("from") LocalDateTime from);

    @Query("SELECT t FROM Ticket t " +
           "LEFT JOIN t.assignedUser au " +
           "LEFT JOIN t.department d " +
           "LEFT JOIN t.category c " +
           "LEFT JOIN t.subcategory sc " +
           "WHERE (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:createdByUserId IS NULL OR t.creator.id = :createdByUserId) " +
           "AND (:assignedToUserId IS NULL OR t.assignedUser.id = :assignedToUserId) " +
           "AND (:source IS NULL OR t.source = :source) " +
           "AND (:query IS NULL " +
           "OR LOWER(t.summary) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(au.username) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(sc.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))")
    Page<Ticket> search(@Param("status") TicketStatus status,
                         @Param("priority") TicketPriority priority,
                         @Param("createdByUserId") Long createdByUserId,
                         @Param("assignedToUserId") Long assignedToUserId,
                         @Param("source") TicketSource source,
                         @Param("query") String query,
                         Pageable pageable);

    // "My Tickets" scope: tickets assigned to me OR offered to me as a candidate.
    // Kept separate from search() above rather than folding a MEMBER OF/candidate
    // clause into it — search() backs the REST API's assignedToUserId filter too,
    // and widening its meaning there would silently change what that endpoint returns.
    @Query("SELECT t FROM Ticket t " +
           "LEFT JOIN t.assignedUser au " +
           "LEFT JOIN t.candidates cand " +
           "LEFT JOIN t.department d " +
           "LEFT JOIN t.category c " +
           "LEFT JOIN t.subcategory sc " +
           "WHERE (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (t.assignedUser.id = :userId OR cand.id = :userId) " +
           "AND (:source IS NULL OR t.source = :source) " +
           "AND (:query IS NULL " +
           "OR LOWER(t.summary) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(au.username) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(d.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) " +
           "OR LOWER(sc.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))")
    Page<Ticket> searchAssignedOrCandidate(@Param("status") TicketStatus status,
                                            @Param("priority") TicketPriority priority,
                                            @Param("userId") Long userId,
                                            @Param("source") TicketSource source,
                                            @Param("query") String query,
                                            Pageable pageable);

    // Atomic claim: succeeds (returns 1) only if the ticket is still unassigned
    // AND the claimant was actually offered it — both checked in the same
    // statement so two concurrent claims can't both win. Native SQL rather than
    // JPQL because JPQL's UPDATE can't target an association column via a
    // subquery EXISTS against a join-table-only relationship this cheaply.
    // clearAutomatically/flushAutomatically matter here (unlike the nullify*
    // methods below, which are never followed by a re-read of the same row in
    // the same transaction): claimTicket() re-fetches the ticket right after
    // these two run, and open-in-view keeps the persistence context alive for
    // the whole request, so a stale cached entity/collection is a real risk.
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE ticket SET assigned_user_id = :userId " +
            "WHERE id = :ticketId AND assigned_user_id IS NULL " +
            "AND EXISTS (SELECT 1 FROM ticket_candidate WHERE ticket_id = :ticketId AND user_id = :userId)",
            nativeQuery = true)
    int claimIfCandidate(@Param("ticketId") Long ticketId, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM ticket_candidate WHERE ticket_id = :ticketId", nativeQuery = true)
    void clearCandidates(@Param("ticketId") Long ticketId);

    @Modifying
    @Query("UPDATE Ticket t SET t.creator = null WHERE t.creator.id = :userId")
    void nullifyCreator(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Ticket t SET t.assignedUser = null WHERE t.assignedUser.id = :userId")
    void nullifyAssignedUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Ticket t SET t.lastModifiedBy = null WHERE t.lastModifiedBy.id = :userId")
    void nullifyLastModifiedBy(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE Ticket t SET t.department = null WHERE t.department.id = :departmentId")
    void nullifyDepartment(@Param("departmentId") Long departmentId);

    @Modifying
    @Query("UPDATE Ticket t SET t.category = null WHERE t.category.id = :categoryId")
    void nullifyCategory(@Param("categoryId") Long categoryId);

    @Modifying
    @Query("UPDATE Ticket t SET t.subcategory = null WHERE t.subcategory.id = :subcategoryId")
    void nullifySubcategory(@Param("subcategoryId") Long subcategoryId);

    long countByCategoryId(Long categoryId);

    long countBySubcategoryId(Long subcategoryId);

    @Query("SELECT t.category.id, COUNT(t) FROM Ticket t WHERE t.category IS NOT NULL GROUP BY t.category.id")
    List<Object[]> countTicketsGroupedByCategory();

    @Query("SELECT t.subcategory.id, COUNT(t) FROM Ticket t WHERE t.subcategory IS NOT NULL GROUP BY t.subcategory.id")
    List<Object[]> countTicketsGroupedBySubcategory();
}
