package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ChangePasswordRequest {
    @NotBlank(message = "Ο τρέχων κωδικός πρόσβασης είναι υποχρεωτικός")
    private String currentPassword;

    @NotBlank(message = "Ο νέος κωδικός πρόσβασης είναι υποχρεωτικός")
    @Size(min = 8, message = "Ο νέος κωδικός πρόσβασης πρέπει να έχει τουλάχιστον 8 χαρακτήρες")
    private String newPassword;

    @NotBlank(message = "Η επιβεβαίωση κωδικού πρόσβασης είναι υποχρεωτική")
    private String confirmPassword;
}
