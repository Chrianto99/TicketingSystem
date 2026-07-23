package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;

@Setter @Getter @Builder
public class SubcategoryResponse {
    private Long id;
    private String name;
    private Long categoryId;
    private String categoryName;
}
