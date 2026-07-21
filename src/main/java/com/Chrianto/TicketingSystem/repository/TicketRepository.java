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
           "AND (:priority IS NULL OR t.priority = :priority)")
    Page<Ticket> search(@Param("status") TicketStatus status,
                         @Param("priority") TicketPriority priority,
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
    @Query("UPDATE Ticket t SET t.problemType = null WHERE t.problemType.id = :problemTypeId")
    void nullifyProblemType(@Param("problemTypeId") Long problemTypeId);
}
