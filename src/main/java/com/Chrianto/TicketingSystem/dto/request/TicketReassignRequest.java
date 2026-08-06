package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter @Getter
public class TicketReassignRequest {

    @NotNull(message = "Ο νέος ανάδοχος χρήστης είναι υποχρεωτικός")
    private Long assignedTo;

    private String commentText;
}
