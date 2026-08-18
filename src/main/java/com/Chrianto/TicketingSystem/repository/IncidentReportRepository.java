package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.IncidentReport;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentReportRepository extends JpaRepository<IncidentReport, Long> {

    Page<IncidentReport> findByStatus(IncidentStatus status, Pageable pageable);
}
