package com.Chrianto.TicketingSystem.dto.request;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

// A "call this person back" ticket — deliberately no department/category:
// TicketService.createCallbackTicket() derives the title from callerName and
// there's no resolution step, so neither applies. Like a regular ticket, it's
// offered to candidates rather than directly assigned — someone still has to
// claim it before calling back.
@Getter @Setter
public class CallbackTicketCreateRequest {
    @NotBlank(message = "Ο αριθμός τηλεφώνου είναι υποχρεωτικός")
    private String phoneNumber;

    @NotBlank(message = "Το όνομα καλούντος είναι υποχρεωτικό")
    private String callerName;

    @NotEmpty(message = "Απαιτείται τουλάχιστον ένας υποψήφιος χρήστης")
    private List<Long> candidateUserIds;

    @NotNull(message = "Η προτεραιότητα είναι υποχρεωτική")
    private TicketPriority priority;

    private String description;
}
