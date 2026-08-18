package com.Chrianto.TicketingSystem.dto.request;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class TicketCreateRequest {
    @NotNull(message = "Ο ανάδοχος χρήστης είναι υποχρεωτικός")
    private Long assignedUserId;

    private String callerName;

    @NotBlank(message = "Ο αριθμός τηλεφώνου είναι υποχρεωτικός")
    private String phoneNumber;

    private Long departmentId;

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
