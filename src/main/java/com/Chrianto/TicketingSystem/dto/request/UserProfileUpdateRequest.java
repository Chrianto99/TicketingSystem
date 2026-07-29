package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserProfileUpdateRequest {
    @NotBlank @Email
    private String email;
}
