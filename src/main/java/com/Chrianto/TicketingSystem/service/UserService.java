package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.UserCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.UserResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().
                stream().
                map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        return toResponse(user);
    }


    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .build();
    }


}
