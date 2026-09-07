package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
    List<Department> findAllByOrderByNameAsc();

    Optional<Department> findByNameIgnoreCase(String name);
}
