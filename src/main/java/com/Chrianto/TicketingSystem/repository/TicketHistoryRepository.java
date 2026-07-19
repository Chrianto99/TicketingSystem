package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketIdOrderByTimestampAsc(Long ticketId);

}

