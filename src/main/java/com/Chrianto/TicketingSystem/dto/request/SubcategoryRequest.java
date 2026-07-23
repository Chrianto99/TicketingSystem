package com.Chrianto.TicketingSystem.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Setter @Getter
public class SubcategoryRequest {
    @NotBlank
    private String name;

    @NotNull
    private Long categoryId;
}
