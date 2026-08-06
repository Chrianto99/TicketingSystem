package com.Chrianto.TicketingSystem.dto.response;

import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import lombok.*;

import java.time.LocalDateTime;

@Setter @Getter @Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private UserRole role;
    private boolean active;
    private LocalDateTime scheduledDeletionAt;
}
