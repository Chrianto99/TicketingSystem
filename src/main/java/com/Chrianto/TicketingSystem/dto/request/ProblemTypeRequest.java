package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter @Getter
public class ProblemTypeRequest {
    @NotBlank
    private String name;
}
