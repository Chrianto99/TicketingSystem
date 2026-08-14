package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserProfileUpdateRequest {
    @Email(message = "Το email πρέπει να είναι έγκυρο")
    private String email;

    private String phoneNumber;

    private String firstName;

    private String lastName;
}
