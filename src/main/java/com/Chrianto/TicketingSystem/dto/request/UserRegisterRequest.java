package com.Chrianto.TicketingSystem.dto.request;


import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import jakarta.validation.constraints.*;

import lombok.*;

@Getter
@Setter
public class UserRegisterRequest {
    @NotBlank
    private String username;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank @Email
    private String email;

    @NotNull
    private UserRole role;
}
