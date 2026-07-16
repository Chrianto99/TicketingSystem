package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {}

