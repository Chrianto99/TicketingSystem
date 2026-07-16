package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

// dto/request/LoginRequest.java
@Getter @Setter
public class UserLoginRequest {
    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
