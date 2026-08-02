package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.ChangePasswordRequest;
import com.Chrianto.TicketingSystem.dto.request.UserProfileUpdateRequest;
import com.Chrianto.TicketingSystem.dto.response.UserResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import com.Chrianto.TicketingSystem.security.SecurityConfig;
import com.Chrianto.TicketingSystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(ProfileViewController.class)
@Import(SecurityConfig.class)
class ProfileViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    // Required to satisfy SecurityConfig's constructor; not exercised since
    // .with(user(...)) sets the SecurityContext directly instead of authenticating.
    @MockitoBean
    private UserDetailsService userDetailsService;

    private User currentUser;
    private UserResponse currentUserResponse;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("Chrianto");
        currentUser.setEmail("Chrianto@ece.auth.gr");
        currentUser.setRole(UserRole.USER);

        currentUserResponse = UserResponse.builder()
                .id(1L)
                .username("Chrianto")
                .email("Chrianto@ece.auth.gr")
                .phoneNumber("6912345678")
                .role(UserRole.USER)
                .active(true)
                .build();

        when(userService.getUserById(1L)).thenReturn(currentUserResponse);
    }

    @Test
    void viewProfile_populatesModelWithCurrentUserAndFormBackingObjects() throws Exception {
        mockMvc.perform(get("/profile")
                        .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attribute("user", currentUserResponse))
                .andExpect(model().attributeExists("userProfileUpdateRequest"))
                .andExpect(model().attributeExists("changePasswordRequest"));
    }

    @Test
    void viewProfile_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(userService);
    }

    @Test
    void updateProfile_withValidData_updatesAndRedirects() throws Exception {
        mockMvc.perform(post("/profile")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("email", "new@ece.auth.gr")
                        .param("phoneNumber", "6987654321"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated=true"));

        verify(userService).updateProfile(eq(1L), any(UserProfileUpdateRequest.class));
    }

    @Test
    void updateProfile_withBlankEmail_reRendersProfileWithFieldErrorAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/profile")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("email", "")
                        .param("phoneNumber", "6987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeHasFieldErrors("userProfileUpdateRequest", "email"))
                .andExpect(model().attribute("user", currentUserResponse))
                .andExpect(model().attributeExists("changePasswordRequest"));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    void updateProfile_withInvalidEmailFormat_reRendersProfileWithFieldErrorAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/profile")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("email", "not-an-email")
                        .param("phoneNumber", "6987654321"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeHasFieldErrors("userProfileUpdateRequest", "email"));

        verify(userService, never()).updateProfile(any(), any());
    }

    @Test
    void updateProfile_withoutAuthentication_redirectsToLoginAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("email", "new@ece.auth.gr")
                        .param("phoneNumber", "6987654321"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(userService);
    }

    @Test
    void updateProfile_withoutCsrfToken_isForbiddenAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/profile")
                        .with(user(currentUser))
                        .param("email", "new@ece.auth.gr")
                        .param("phoneNumber", "6987654321"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void changePassword_withValidData_changesAndRedirects() throws Exception {
        mockMvc.perform(post("/profile/password")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("currentPassword", "qwerty123")
                        .param("newPassword", "newpassword1")
                        .param("confirmPassword", "newpassword1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordChanged=true"));

        verify(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_withBlankCurrentPassword_reRendersProfileWithFieldErrorAndDoesNotChange() throws Exception {
        mockMvc.perform(post("/profile/password")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("currentPassword", "")
                        .param("newPassword", "newpassword1")
                        .param("confirmPassword", "newpassword1"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeHasFieldErrors("changePasswordRequest", "currentPassword"))
                .andExpect(model().attribute("user", currentUserResponse))
                .andExpect(model().attributeExists("userProfileUpdateRequest"));

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    void changePassword_withTooShortNewPassword_reRendersProfileWithFieldErrorAndDoesNotChange() throws Exception {
        mockMvc.perform(post("/profile/password")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("currentPassword", "qwerty123")
                        .param("newPassword", "short")
                        .param("confirmPassword", "short"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/profile"))
                .andExpect(model().attributeHasFieldErrors("changePasswordRequest", "newPassword"));

        verify(userService, never()).changePassword(any(), any());
    }

    @Test
    void changePassword_whenCurrentPasswordIncorrect_redirectsBackWithFlashError() throws Exception {
        doThrow(new IllegalArgumentException("Current password is incorrect"))
                .when(userService).changePassword(eq(1L), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/profile/password")
                        .with(user(currentUser))
                        .with(csrf())
                        .header("Referer", "/profile")
                        .param("currentPassword", "wrongpassword")
                        .param("newPassword", "newpassword1")
                        .param("confirmPassword", "newpassword1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile"))
                .andExpect(flash().attribute("errorMessage", "Current password is incorrect"));
    }

    @Test
    void changePassword_withoutAuthentication_redirectsToLoginAndDoesNotChange() throws Exception {
        mockMvc.perform(post("/profile/password")
                        .with(csrf())
                        .param("currentPassword", "qwerty123")
                        .param("newPassword", "newpassword1")
                        .param("confirmPassword", "newpassword1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(userService);
    }

    @Test
    void changePassword_withoutCsrfToken_isForbiddenAndDoesNotChange() throws Exception {
        mockMvc.perform(post("/profile/password")
                        .with(user(currentUser))
                        .param("currentPassword", "qwerty123")
                        .param("newPassword", "newpassword1")
                        .param("confirmPassword", "newpassword1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }
}
