package com.Chrianto.TicketingSystem.dto.request;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter @Setter
public class TicketCreateRequest {
    // A new ticket is always offered, never directly assigned — even a single
    // person still has to claim it. Non-empty is enforced manually (not
    // @NotEmpty) so the error message can be composed alongside other checks.
    private List<Long> candidateUserIds;

    private String callerName;

    @NotBlank(message = "Ο αριθμός τηλεφώνου είναι υποχρεωτικός")
    private String phoneNumber;

    // Free text rather than an FK — a name that doesn't match an existing
    // department creates one on the fly (see TicketService#createTicket).
    private String departmentName;

    @NotNull(message = "Η κατηγορία βλάβης είναι υποχρεωτική")
    private Long categoryId;

    private Long subcategoryId;

    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$",
            message = "Η διεύθυνση IP πρέπει να ακολουθεί τη μορφή IPv4"
    )
    private String ipAddress;

    private String summary;

    @NotNull(message = "Η προτεραιότητα είναι υποχρεωτική")
    private TicketPriority priority;

    private String description;

    private String resolution;


}
