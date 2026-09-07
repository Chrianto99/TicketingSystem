package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Ticket creation as spawned from an Incident — deliberately a much
// smaller field set than TicketCreateRequest (no department/category/priority):
// creator and timestamps are server-set, the related incident comes from the
// path, and priority is inherited from that incident rather than resubmitted,
// so this is just what the caller actually supplies. Like a regular ticket,
// it's offered to candidates rather than directly assigned.
@Getter @Setter
public class IncidentTicketCreateRequest {
    @NotEmpty(message = "Απαιτείται τουλάχιστον ένας υποψήφιος χρήστης")
    private List<Long> candidateUserIds;

    @NotBlank(message = "Ο τίτλος είναι υποχρεωτικός")
    private String title;

    private String description;
}
