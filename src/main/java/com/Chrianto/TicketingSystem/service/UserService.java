package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.ChangePasswordRequest;
import com.Chrianto.TicketingSystem.dto.request.UserProfileUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.UserRegisterRequest;
import com.Chrianto.TicketingSystem.dto.response.UserResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.AttachmentRepository;
import com.Chrianto.TicketingSystem.repository.CommentRepository;
import com.Chrianto.TicketingSystem.repository.TicketHistoryRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
import com.Chrianto.TicketingSystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_PASSWORD = "qwerty123";
    private static final int DEACTIVATION_RETENTION_DAYS = 7;

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final CommentRepository commentRepository;
    private final AttachmentRepository attachmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse registerUser(UserRegisterRequest req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setSpecialization(req.getSpecialization());
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setRole(req.getRole());

        user = userRepository.save(user);
        return toResponse(user);
    }

    public void resetPassword(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + userId));

        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userRepository.save(user);
    }

    public UserResponse updateProfile(Long userId, UserProfileUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + userId));

        user.setEmail(req.getEmail());
        user.setPhoneNumber(req.getPhoneNumber());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setSpecialization(req.getSpecialization());
        user = userRepository.save(user);
        return toResponse(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + userId));

        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Ο τρέχων κωδικός πρόσβασης είναι λανθασμένος");
        }
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new IllegalArgumentException("Ο νέος κωδικός πρόσβασης και η επιβεβαίωση δεν ταιριάζουν");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().
                stream().
                map(this::toResponse)
                .toList();
    }

    public UserResponse getUserById(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + userId));

        return toResponse(user);
    }

    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + userId));

        user.setActive(false);
        user.setScheduledDeletionAt(LocalDateTime.now().plusDays(DEACTIVATION_RETENTION_DAYS));
        userRepository.save(user);
    }

    @Transactional
    public void reactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + userId));

        user.setActive(true);
        user.setScheduledDeletionAt(null);
        userRepository.save(user);
    }

    @Transactional
    public void toggleAdminRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + userId));

        user.setRole(user.getRole() == UserRole.ADMIN ? UserRole.USER : UserRole.ADMIN);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Δεν βρέθηκε χρήστης με id: " + userId);
        }

        ticketRepository.nullifyCreator(userId);
        ticketRepository.nullifyAssignedUser(userId);
        ticketRepository.nullifyLastModifiedBy(userId);
        ticketHistoryRepository.nullifyPerformedBy(userId);
        ticketHistoryRepository.nullifyAssignedTo(userId);
        commentRepository.nullifyAuthor(userId);
        attachmentRepository.nullifyUploadedBy(userId);

        userRepository.deleteById(userId);
    }


    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .specialization(u.getSpecialization())
                .role(u.getRole())
                .active(u.isActive())
                .scheduledDeletionAt(u.getScheduledDeletionAt())
                .build();
    }


}
