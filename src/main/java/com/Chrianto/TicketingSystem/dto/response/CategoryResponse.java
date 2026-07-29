package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;

@Setter @Getter @Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private boolean active;
}
