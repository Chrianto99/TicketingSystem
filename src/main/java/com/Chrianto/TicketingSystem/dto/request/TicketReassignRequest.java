package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter @Getter
public class TicketReassignRequest {

    @NonNull
    private Long performedBy;

    @NonNull
    private Long assignedTo;

    @NotBlank
    private String commentText;
}
