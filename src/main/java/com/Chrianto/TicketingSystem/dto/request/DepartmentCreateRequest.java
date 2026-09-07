package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class DepartmentCreateRequest {
    @NotBlank(message = "Το όνομα είναι υποχρεωτικό")
    private String name;

    private String location;
}
