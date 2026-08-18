package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class IncidentCommentCreateRequest {
    @NotBlank(message = "Το σχόλιο δεν πρέπει να είναι κενό")
    private String commentText;
}
