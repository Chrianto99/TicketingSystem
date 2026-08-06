package com.Chrianto.TicketingSystem.dto.request;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Setter @Getter
public class TicketUpdateRequest {

    private String callerName;

    private Long departmentId;

    private Long categoryId;

    private Long subcategoryId;

    private String phoneNumber;

    private String summary;

    private String description;

    @NotNull(message = "Η προτεραιότητα είναι υποχρεωτική")
    private TicketPriority priority;

    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$",
            message = "Η διεύθυνση IP πρέπει να ακολουθεί τη μορφή IPv4"
    )
    private String ipAddress;



}
