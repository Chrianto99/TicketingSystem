package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;

@Getter @Setter
public class TicketOfferRequest {

    @NotEmpty(message = "Απαιτείται τουλάχιστον ένας υποψήφιος χρήστης")
    private List<Long> candidateUserIds;
}
