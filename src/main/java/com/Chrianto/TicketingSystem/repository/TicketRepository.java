package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Ticket;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("SELECT t FROM Ticket t " +
           "WHERE (:status IS NULL OR t.status = :status) " +
           "AND (:priority IS NULL OR t.priority = :priority) " +
           "AND (:departmentId IS NULL OR t.department.id = :departmentId) " +
           "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
           "AND (:createdByUserId IS NULL OR t.creator.id = :createdByUserId) " +
           "AND (:assignedToUserId IS NULL OR t.assignedUser.id = :assignedToUserId)")
    Page<Ticket> search(@Param("status") TicketStatus status,
                         @Param("priority") TicketPriority priority,
                         @Param("departmentId") Long departmentId,
                         @Param("categoryId") Long categoryId,
                         @Param("createdByUserId") Long createdByUserId,
                         @Param("assignedToUserId") Long assignedToUserId,
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
}
