package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class TicketResolveRequest {

    @NonNull
    private Long performedBy;

    @NotBlank
    private String commentText;


}
