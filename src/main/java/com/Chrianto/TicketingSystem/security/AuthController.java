package com.Chrianto.TicketingSystem.security;

import com.Chrianto.TicketingSystem.dto.request.UserRegisterRequest;
import com.Chrianto.TicketingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> register(@Valid @RequestBody UserRegisterRequest req) {
        userService.registerUser(req);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
