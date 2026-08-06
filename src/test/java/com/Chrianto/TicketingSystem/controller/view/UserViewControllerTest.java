package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.UserRegisterRequest;
import com.Chrianto.TicketingSystem.dto.response.UserResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import com.Chrianto.TicketingSystem.security.SecurityConfig;
import com.Chrianto.TicketingSystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(UserViewController.class)
@Import(SecurityConfig.class)
class UserViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    // Required to satisfy SecurityConfig's constructor; not exercised since
    // .with(user(...)) sets the SecurityContext directly instead of authenticating.
    @MockitoBean
    private UserDetailsService userDetailsService;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@ticketing.com");
        adminUser.setRole(UserRole.ADMIN);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("Chrianto");
        regularUser.setEmail("Chrianto@ece.auth.gr");
        regularUser.setRole(UserRole.USER);

        when(userService.getAllUsers()).thenReturn(List.of());
    }

    @Test
    void listUsers_populatesModelWithAllUsers() throws Exception {
        UserResponse response = UserResponse.builder().id(5L).username("Chrianto").active(true).build();
        when(userService.getAllUsers()).thenReturn(List.of(response));

        mockMvc.perform(get("/users")
                        .with(user(regularUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users/list"))
                .andExpect(model().attribute("users", List.of(response)));
    }

    @Test
    void listUsers_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(userService);
    }

    @Test
    void newUserForm_asAdmin_populatesModelWithRequestAndRoles() throws Exception {
        mockMvc.perform(get("/users/new")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeExists("userRegisterRequest"))
                .andExpect(model().attribute("roles", UserRole.values()));
    }

    @Test
    void newUserForm_asNonAdmin_isForbidden() throws Exception {
        mockMvc.perform(get("/users/new")
                        .with(user(regularUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createUser_asAdminWithValidData_registersUserAndRedirects() throws Exception {
        mockMvc.perform(post("/users")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("username", "newbie")
                        .param("email", "newbie@ticketing.com")
                        .param("phoneNumber", "6912345678")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?registered=true"));

        verify(userService).registerUser(any(UserRegisterRequest.class));
    }

    @Test
    void createUser_withBlankEmailAndPhone_registersUserWithNullContactInfo() throws Exception {
        mockMvc.perform(post("/users")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("username", "no_contact_info_user")
                        .param("email", "")
                        .param("phoneNumber", "")
                        .param("role", "USER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?registered=true"));

        ArgumentCaptor<UserRegisterRequest> captor = ArgumentCaptor.forClass(UserRegisterRequest.class);
        verify(userService).registerUser(captor.capture());
        assertThat(captor.getValue().getEmail()).isNull();
        assertThat(captor.getValue().getPhoneNumber()).isNull();
    }

    @Test
    void createUser_withInvalidEmailFormat_reRendersFormWithFieldErrorAndDoesNotRegister() throws Exception {
        mockMvc.perform(post("/users")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("username", "newbie")
                        .param("email", "not-an-email")
                        .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeHasFieldErrors("userRegisterRequest", "email"));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void createUser_withBlankUsername_reRendersFormWithFieldErrorAndDoesNotRegister() throws Exception {
        mockMvc.perform(post("/users")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("username", "")
                        .param("email", "newbie@ticketing.com")
                        .param("phoneNumber", "6912345678")
                        .param("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("users/form"))
                .andExpect(model().attributeHasFieldErrors("userRegisterRequest", "username"))
                .andExpect(model().attribute("roles", UserRole.values()));

        verify(userService, never()).registerUser(any());
    }

    @Test
    void createUser_asNonAdmin_isForbiddenAndDoesNotRegister() throws Exception {
        mockMvc.perform(post("/users")
                        .with(user(regularUser))
                        .with(csrf())
                        .param("username", "newbie")
                        .param("email", "newbie@ticketing.com")
                        .param("phoneNumber", "6912345678")
                        .param("role", "USER"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void createUser_withoutCsrfToken_isForbiddenAndDoesNotRegister() throws Exception {
        mockMvc.perform(post("/users")
                        .with(user(adminUser))
                        .param("username", "newbie")
                        .param("email", "newbie@ticketing.com")
                        .param("phoneNumber", "6912345678")
                        .param("role", "USER"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void resetPassword_asAdmin_resetsAndRedirects() throws Exception {
        mockMvc.perform(post("/users/5/reset-password")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?passwordReset=true"));

        verify(userService).resetPassword(5L);
    }

    @Test
    void resetPassword_asNonAdmin_isForbiddenAndDoesNotReset() throws Exception {
        mockMvc.perform(post("/users/5/reset-password")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void resetPassword_withoutCsrfToken_isForbiddenAndDoesNotReset() throws Exception {
        mockMvc.perform(post("/users/5/reset-password")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteUser_asAdmin_deactivatesRatherThanHardDeletingAndRedirects() throws Exception {
        mockMvc.perform(post("/users/5/delete")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?deactivated=true"));

        verify(userService).deactivateUser(5L);
        verify(userService, never()).deleteUser(any());
    }

    @Test
    void deleteUser_asNonAdmin_isForbiddenAndDoesNotDeactivate() throws Exception {
        mockMvc.perform(post("/users/5/delete")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void deleteUser_withoutCsrfToken_isForbiddenAndDoesNotDeactivate() throws Exception {
        mockMvc.perform(post("/users/5/delete")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void reactivateUser_asAdmin_reactivatesAndRedirects() throws Exception {
        mockMvc.perform(post("/users/5/reactivate")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?reactivated=true"));

        verify(userService).reactivateUser(5L);
    }

    @Test
    void reactivateUser_asNonAdmin_isForbiddenAndDoesNotReactivate() throws Exception {
        mockMvc.perform(post("/users/5/reactivate")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void reactivateUser_withoutCsrfToken_isForbiddenAndDoesNotReactivate() throws Exception {
        mockMvc.perform(post("/users/5/reactivate")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void toggleAdmin_asAdminOnOtherUser_togglesRoleAndRedirects() throws Exception {
        mockMvc.perform(post("/users/5/toggle-admin")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?roleChanged=true"));

        verify(userService).toggleAdminRole(5L);
    }

    @Test
    void toggleAdmin_onOwnAccount_doesNotToggleAndRedirectsWithError() throws Exception {
        mockMvc.perform(post("/users/" + adminUser.getId() + "/toggle-admin")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/users?roleChangeError=true"));

        verify(userService, never()).toggleAdminRole(any());
    }

    @Test
    void toggleAdmin_asNonAdmin_isForbiddenAndDoesNotToggle() throws Exception {
        mockMvc.perform(post("/users/5/toggle-admin")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void toggleAdmin_withoutCsrfToken_isForbiddenAndDoesNotToggle() throws Exception {
        mockMvc.perform(post("/users/5/toggle-admin")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }
}
