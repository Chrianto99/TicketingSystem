package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter @Getter
public class TicketReassignRequest {

    @NotNull
    private Long assignedTo;

    @NotBlank
    private String commentText;
}
