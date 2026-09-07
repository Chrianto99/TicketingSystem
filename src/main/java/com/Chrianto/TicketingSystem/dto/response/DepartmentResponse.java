package com.Chrianto.TicketingSystem.dto.response;

import lombok.*;

@Getter @Setter @Builder
public class DepartmentResponse {
    private Long id;
    private String name;
    private boolean active;
    private String location;
}
