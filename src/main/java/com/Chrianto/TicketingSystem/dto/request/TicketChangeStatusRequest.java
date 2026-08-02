package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class TicketChangeStatusRequest {

    @NotBlank(message = "Το σχόλιο είναι υποχρεωτικό")
    private String commentText;

    private Long subcategoryId;

}
