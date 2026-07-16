package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {}

