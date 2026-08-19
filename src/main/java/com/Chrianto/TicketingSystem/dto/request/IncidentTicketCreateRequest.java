package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// Ticket creation as spawned from an Incident — deliberately a much
// smaller field set than TicketCreateRequest (no department/category/priority):
// creator and timestamps are server-set, the related incident comes from the
// path, and priority is inherited from that incident rather than resubmitted,
// so this is just what the caller actually supplies.
@Getter @Setter
public class IncidentTicketCreateRequest {
    @NotNull(message = "Ο ανάδοχος χρήστης είναι υποχρεωτικός")
    private Long assignedUserId;

    @NotBlank(message = "Ο τίτλος είναι υποχρεωτικός")
    private String title;

    private String description;
}
