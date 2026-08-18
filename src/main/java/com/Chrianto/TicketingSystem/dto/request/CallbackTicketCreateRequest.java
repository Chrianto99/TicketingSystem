package com.Chrianto.TicketingSystem.dto.request;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

// A "call this person back" ticket — deliberately no department/category:
// TicketService.createCallbackTicket() derives the title from callerName and
// there's no resolution step, so neither applies.
@Getter @Setter
public class CallbackTicketCreateRequest {
    @NotBlank(message = "Ο αριθμός τηλεφώνου είναι υποχρεωτικός")
    private String phoneNumber;

    @NotBlank(message = "Το όνομα καλούντος είναι υποχρεωτικό")
    private String callerName;

    @NotNull(message = "Ο ανάδοχος χρήστης είναι υποχρεωτικός")
    private Long assignedUserId;

    @NotNull(message = "Η προτεραιότητα είναι υποχρεωτική")
    private TicketPriority priority;

    private String description;
}
