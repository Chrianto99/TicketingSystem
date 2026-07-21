package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Setter @Getter
public class TicketUpdateRequest {

    private String callerName;

    private Long departmentId;

    private Long problemTypeId;

    private String phoneNumber;

    private String description;

    @Pattern(
            regexp = "^((25[0-5]|2[0-4]\\d|[01]?\\d?\\d)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d?\\d)$",
            message = "IP address must follow IPv4 pattern"
    )
    private String ipAddress;



}
