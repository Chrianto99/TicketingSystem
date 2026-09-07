package com.Chrianto.TicketingSystem.repository;

import com.Chrianto.TicketingSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    List<User> findByIsActiveTrueOrderByUsernameAsc();

    boolean existsByIdAndUnseenAssignedTicketsTrue(Long id);

    @Modifying
    @Query("UPDATE User u SET u.unseenAssignedTickets = true WHERE u.id = :userId")
    void markUnseenAssignedTickets(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE User u SET u.unseenAssignedTickets = false WHERE u.id = :userId")
    void clearUnseenAssignedTickets(@Param("userId") Long userId);
}

