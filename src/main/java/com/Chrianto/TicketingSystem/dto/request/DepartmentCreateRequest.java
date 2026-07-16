package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class DepartmentCreateRequest {
    @NotBlank
    private String name;

//    private String defaultNumber; // optional
}
