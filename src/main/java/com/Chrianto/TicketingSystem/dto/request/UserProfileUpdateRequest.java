package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserProfileUpdateRequest {
    @NotBlank(message = "Το email είναι υποχρεωτικό") @Email(message = "Το email πρέπει να είναι έγκυρο")
    private String email;

    @NotBlank(message = "Ο αριθμός τηλεφώνου είναι υποχρεωτικός")
    private String phoneNumber;
}
