package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
