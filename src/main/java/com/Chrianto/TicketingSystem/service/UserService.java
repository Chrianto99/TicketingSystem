package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.UserRegisterRequest;
import com.Chrianto.TicketingSystem.dto.response.UserResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.CommentRepository;
import com.Chrianto.TicketingSystem.repository.TicketHistoryRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
import com.Chrianto.TicketingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final CommentRepository commentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse registerUser(UserRegisterRequest req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());

        user = userRepository.save(user);
        return toResponse(user);
    }

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

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }

        ticketRepository.nullifyCreator(userId);
        ticketRepository.nullifyAssignedUser(userId);
        ticketRepository.nullifyLastModifiedBy(userId);
        ticketHistoryRepository.nullifyPerformedBy(userId);
        ticketHistoryRepository.nullifyAssignedTo(userId);
        commentRepository.nullifyAuthor(userId);

        userRepository.deleteById(userId);
    }


    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .build();
    }


}
