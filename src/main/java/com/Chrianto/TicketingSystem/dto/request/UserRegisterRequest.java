package com.Chrianto.TicketingSystem.dto.request;


import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import jakarta.validation.constraints.*;

import lombok.*;

@Getter
@Setter
public class UserRegisterRequest {
    @NotBlank(message = "Το όνομα χρήστη είναι υποχρεωτικό")
    private String username;

    @Email(message = "Το email πρέπει να είναι έγκυρο")
    private String email;

    private String phoneNumber;

    @NotNull(message = "Ο ρόλος είναι υποχρεωτικός")
    private UserRole role;
}
