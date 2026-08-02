package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CommentUpdateRequest {

    @NotBlank(message = "Το κείμενο είναι υποχρεωτικό")
    private String text;
}
