package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class TicketChangeStatusRequest {

    @NonNull
    private Long performedBy;

    @NotBlank
    private String commentText;


}
