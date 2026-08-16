package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;

@Getter @Setter @Builder
public class EntityShareResponse {
    private Long id;
    private String name;
    private long ticketCount;
    private double percentage;
    // Set only on subcategory shares: the id of the owning category, so the
    // frontend can nest each subcategory's bar under its parent's row.
    private Long parentId;
}
