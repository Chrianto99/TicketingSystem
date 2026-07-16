package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.UserCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.dto.response.UserResponse;
import com.Chrianto.TicketingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getTicketById(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserById(userId));
    }
}
