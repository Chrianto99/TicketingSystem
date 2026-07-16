package com.Chrianto.TicketingSystem.dto.response;

import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import lombok.*;

@Getter @Setter @Builder
public class LoginResponse {
    private String token;
    private String username;
    private UserRole role;
}

