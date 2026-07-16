package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {}

