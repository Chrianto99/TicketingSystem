package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;

@Getter @Setter @Builder
public class TicketStatsPointResponse {
    private String period;
    private long created;
    private long resolved;
    private long cancelled;
}
