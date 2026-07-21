package com.Chrianto.TicketingSystem.dto.request;

import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class TicketCreateRequest {
    @NotNull
    private Long assignedUserId;

    @NotBlank
    private String callerName;

    @NotBlank
    private String phoneNumber;

    @NotNull
    private Long departmentId;

    @NotNull
    private Long problemTypeId;

    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$",
            message = "IP address must follow IPv4 pattern"
    )
    private String ipAddress;

    @NotBlank
    private String description;

    @NotNull
    private TicketPriority priority;

    @NotBlank
    private String commentText;


}
