package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter @Getter
public class CategoryRequest {
    @NotBlank(message = "Το όνομα είναι υποχρεωτικό")
    private String name;
}
