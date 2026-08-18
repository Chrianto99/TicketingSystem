package com.Chrianto.TicketingSystem.dto.request;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class IncidentReportUpdateRequest {
    private String subject;

    @NotBlank(message = "Η περιγραφή είναι υποχρεωτική")
    private String description;

    @NotNull(message = "Η προτεραιότητα είναι υποχρεωτική")
    private TicketPriority priority;
}
