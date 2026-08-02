package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter @Getter
public class SubcategoryRequest {
    @NotBlank(message = "Το όνομα είναι υποχρεωτικό")
    private String name;

    @NotNull(message = "Η κατηγορία βλάβης είναι υποχρεωτική")
    private Long categoryId;
}
