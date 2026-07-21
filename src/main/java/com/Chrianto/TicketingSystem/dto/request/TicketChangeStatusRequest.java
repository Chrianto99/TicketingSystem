package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter
public class TicketChangeStatusRequest {

    @NotBlank
    private String commentText;


}
