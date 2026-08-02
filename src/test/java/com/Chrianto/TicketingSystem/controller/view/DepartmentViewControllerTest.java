package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.DepartmentCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.DepartmentResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import com.Chrianto.TicketingSystem.security.SecurityConfig;
import com.Chrianto.TicketingSystem.service.DepartmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

@WebMvcTest(DepartmentViewController.class)
@Import(SecurityConfig.class)
class DepartmentViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DepartmentService departmentService;
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

        when(departmentService.getAllDepartments()).thenReturn(List.of());
    }

    @Test
    void listDepartments_splitsResponsesIntoActiveAndInactiveModelAttributes() throws Exception {
        DepartmentResponse active = DepartmentResponse.builder().id(1L).name("IT").active(true).build();
        DepartmentResponse inactive = DepartmentResponse.builder().id(2L).name("Legacy").active(false).build();
        when(departmentService.getAllDepartments()).thenReturn(List.of(active, inactive));

        mockMvc.perform(get("/departments")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("departments/list"))
                .andExpect(model().attribute("activeDepartments", List.of(active)))
                .andExpect(model().attribute("inactiveDepartments", List.of(inactive)))
                .andExpect(model().attributeExists("departmentCreateRequest"));
    }

    @Test
    void listDepartments_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/departments"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(departmentService);
    }

    @Test
    void createDepartment_asAdminWithValidData_createsDepartmentAndRedirects() throws Exception {
        mockMvc.perform(post("/departments")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", "IT Support")
                        .param("code", "IT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/departments"));

        verify(departmentService).createDepartment(any(DepartmentCreateRequest.class));
    }

    @Test
    void createDepartment_withBlankName_redirectsWithoutCreating() throws Exception {
        mockMvc.perform(post("/departments")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", "")
                        .param("code", "IT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/departments"));

        verify(departmentService, never()).createDepartment(any());
    }

    @Test
    void createDepartment_asNonAdmin_isForbiddenAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/departments")
                        .with(user(regularUser))
                        .with(csrf())
                        .param("name", "IT Support")
                        .param("code", "IT"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(departmentService);
    }

    @Test
    void createDepartment_withoutAuthentication_redirectsToLoginAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/departments")
                        .with(csrf())
                        .param("name", "IT Support")
                        .param("code", "IT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(departmentService);
    }

    @Test
    void createDepartment_withoutCsrfToken_isForbiddenAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/departments")
                        .with(user(adminUser))
                        .param("name", "IT Support")
                        .param("code", "IT"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(departmentService);
    }

    @Test
    void editDepartment_asAdminWithValidData_updatesDepartmentAndRedirects() throws Exception {
        mockMvc.perform(post("/departments/5/edit")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", "Renamed Department")
                        .param("code", "RD"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/departments"));

        verify(departmentService).updateDepartment(5L, "Renamed Department", "RD");
    }

    @Test
    void editDepartment_withoutOptionalCode_updatesDepartmentWithNullCode() throws Exception {
        mockMvc.perform(post("/departments/5/edit")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", "Renamed Department"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/departments"));

        verify(departmentService).updateDepartment(eq(5L), eq("Renamed Department"), isNull());
    }

    @Test
    void editDepartment_asNonAdmin_isForbiddenAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/departments/5/edit")
                        .with(user(regularUser))
                        .with(csrf())
                        .param("name", "Renamed Department")
                        .param("code", "RD"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(departmentService);
    }

    @Test
    void editDepartment_withoutCsrfToken_isForbiddenAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/departments/5/edit")
                        .with(user(adminUser))
                        .param("name", "Renamed Department")
                        .param("code", "RD"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(departmentService);
    }

    @Test
    void deleteDepartment_asAdmin_deletesAndRedirects() throws Exception {
        mockMvc.perform(post("/departments/5/delete")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/departments"));

        verify(departmentService).deleteDepartment(5L);
    }

    @Test
    void deleteDepartment_asNonAdmin_isForbiddenAndDoesNotDelete() throws Exception {
        mockMvc.perform(post("/departments/5/delete")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(departmentService);
    }

    @Test
    void deleteDepartment_withoutCsrfToken_isForbiddenAndDoesNotDelete() throws Exception {
        mockMvc.perform(post("/departments/5/delete")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(departmentService);
    }

    @Test
    void toggleDepartmentActive_asAdmin_togglesAndRedirects() throws Exception {
        mockMvc.perform(post("/departments/5/toggle-active")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/departments"));

        verify(departmentService).toggleDepartmentActiveState(5L);
    }

    @Test
    void toggleDepartmentActive_asNonAdmin_isForbiddenAndDoesNotToggle() throws Exception {
        mockMvc.perform(post("/departments/5/toggle-active")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(departmentService);
    }

    @Test
    void toggleDepartmentActive_withoutCsrfToken_isForbiddenAndDoesNotToggle() throws Exception {
        mockMvc.perform(post("/departments/5/toggle-active")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(departmentService);
    }
}
