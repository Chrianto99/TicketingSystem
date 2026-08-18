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

    List<Ticket> findByIncidentReportIdOrderByCreatedAtDesc(Long incidentReportId);

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
