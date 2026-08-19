package com.Chrianto.TicketingSystem.dto.response;

import com.Chrianto.TicketingSystem.entity.enums.IncidentAction;
import lombok.*;

import java.time.LocalDateTime;

// Display-ready shape for the server-rendered Ιστορικό tab on the incident
// detail page: icon/mainText/quote are pre-composed here (not in Thymeleaf)
// so the template stays a plain th:each with no per-action branching.
@Getter @Setter @Builder
public class IncidentHistoryResponse {
    private Long id;
    private IncidentAction action;
    private String icon;
    private String iconCssClass;
    private String performedByUsername;
    private String mainText;
    private String quote;
    private LocalDateTime timestamp;
}
