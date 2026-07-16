package com.Chrianto.TicketingSystem.dto.request;


import lombok.*;

@Getter @Setter
public class UserCreateRequest {
    @NonNull
    private Long id;

    @NonNull
    String username;


}
