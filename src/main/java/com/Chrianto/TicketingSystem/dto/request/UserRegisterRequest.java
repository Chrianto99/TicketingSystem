package com.Chrianto.TicketingSystem.dto.request;


import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import jakarta.validation.constraints.*;

import lombok.*;

@Getter
@Setter
public class UserRegisterRequest {
    @NotBlank
    private String username;

    @NotBlank @Email
    private String email;

    @NotBlank
    private String phoneNumber;

    @NotNull
    private UserRole role;
}
