package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Incident;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    @Query("SELECT i FROM Incident i " +
           "WHERE (:status IS NULL OR i.status = :status) " +
           "AND (:priority IS NULL OR i.priority = :priority) " +
           "AND (:query IS NULL OR LOWER(i.subject) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')))")
    Page<Incident> search(@Param("status") IncidentStatus status,
                                 @Param("priority") TicketPriority priority,
                                 @Param("query") String query,
                                 Pageable pageable);
}
