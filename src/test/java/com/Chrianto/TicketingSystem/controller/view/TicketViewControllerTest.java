package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.TicketChangeStatusRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketReassignRequest;
import com.Chrianto.TicketingSystem.dto.response.TicketResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import com.Chrianto.TicketingSystem.security.SecurityConfig;
import com.Chrianto.TicketingSystem.service.CategoryService;
import com.Chrianto.TicketingSystem.service.DepartmentService;
import com.Chrianto.TicketingSystem.service.TicketHistoryService;
import com.Chrianto.TicketingSystem.service.TicketService;
import com.Chrianto.TicketingSystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

@WebMvcTest(TicketViewController.class)
@Import(SecurityConfig.class)
class TicketViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;
    @MockitoBean
    private TicketHistoryService ticketHistoryService;
    @MockitoBean
    private DepartmentService departmentService;
    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private UserService userService;
    // Required to satisfy SecurityConfig's constructor; not exercised since
    // .with(user(...)) sets the SecurityContext directly instead of authenticating.
    @MockitoBean
    private UserDetailsService userDetailsService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("admin");
        currentUser.setEmail("admin@ticketing.com");
        currentUser.setRole(UserRole.ADMIN);

        when(departmentService.getAllDepartments()).thenReturn(List.of());
        when(categoryService.getAllCategories()).thenReturn(List.of());
        when(userService.getAllUsers()).thenReturn(List.of());
        // The re-rendered "tickets/list" view (shown on validation failure) also
        // renders the ticket table, so getAllTickets must be stubbed for every test.
        when(ticketService.getAllTickets(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void listTickets_withAllStatusAndAllScope_exposesRoundTrippableParamsForPagination() throws Exception {
        // Regression test: pagination links are built from statusParam/scopeParam, not the
        // raw status/scope model attributes — those are null here (meaning "no filter" for
        // the JPA query), and a null-valued Thymeleaf @{} param is dropped from the URL
        // entirely, which used to silently reset the page back to the "My Tickets" / OPEN
        // default the moment a user on "All Tickets" clicked to another page.
        mockMvc.perform(get("/tickets")
                        .with(user(currentUser))
                        .param("status", "ALL")
                        .param("scope", "all"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("status", (Object) null))
                .andExpect(model().attribute("statusParam", "ALL"))
                .andExpect(model().attribute("scope", (Object) null))
                .andExpect(model().attribute("scopeParam", "all"));
    }

    @Test
    void listTickets_withDefaultParams_exposesResolvedDefaultsAsParams() throws Exception {
        mockMvc.perform(get("/tickets")
                        .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("statusParam", "OPEN"))
                .andExpect(model().attribute("scopeParam", "assigned"));
    }

    @Test
    void createTicket_withValidData_createsTicketAndRedirectsToDetail() throws Exception {
        TicketResponse response = TicketResponse.builder().id(42L).build();
        when(ticketService.createTicket(any(TicketCreateRequest.class), eq(currentUser))).thenReturn(response);

        mockMvc.perform(post("/tickets")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("assignedUserId", "1")
                        .param("description", "Printer is on fire")
                        .param("callerName", "Jane Doe")
                        .param("phoneNumber", "6912345678")
                        .param("departmentId", "1")
                        .param("categoryId", "1")
                        .param("priority", "HIGH")
                        .param("commentText", "Initial report"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=42"));

        verify(ticketService).createTicket(any(TicketCreateRequest.class), eq(currentUser));
    }

    @Test
    void createTicket_withBlankDescription_reRendersFormWithFieldErrorAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/tickets")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("assignedUserId", "1")
                        .param("description", "")
                        .param("callerName", "Jane Doe")
                        .param("phoneNumber", "6912345678")
                        .param("departmentId", "1")
                        .param("categoryId", "1")
                        .param("priority", "HIGH")
                        .param("commentText", "Initial report"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/list"))
                .andExpect(model().attributeHasFieldErrors("ticketCreateRequest", "description"));

        verify(ticketService, never()).createTicket(any(), any());
    }

    @Test
    void createTicket_missingAssignedUser_reRendersFormWithFieldErrorAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/tickets")
                        .with(user(currentUser))
                        .with(csrf())
                        // assignedUserId intentionally omitted
                        .param("description", "Printer is on fire")
                        .param("callerName", "Jane Doe")
                        .param("phoneNumber", "6912345678")
                        .param("departmentId", "1")
                        .param("categoryId", "1")
                        .param("priority", "HIGH")
                        .param("commentText", "Initial report"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/list"))
                .andExpect(model().attributeHasFieldErrors("ticketCreateRequest", "assignedUserId"));

        verify(ticketService, never()).createTicket(any(), any());
    }

    @Test
    void createTicket_withInvalidIpAddress_reRendersFormWithFieldErrorAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/tickets")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("assignedUserId", "1")
                        .param("description", "Printer is on fire")
                        .param("callerName", "Jane Doe")
                        .param("phoneNumber", "6912345678")
                        .param("departmentId", "1")
                        .param("categoryId", "1")
                        .param("priority", "HIGH")
                        .param("commentText", "Initial report")
                        .param("ipAddress", "not-an-ip"))
                .andExpect(status().isOk())
                .andExpect(view().name("tickets/list"))
                .andExpect(model().attributeHasFieldErrors("ticketCreateRequest", "ipAddress"));

        verify(ticketService, never()).createTicket(any(), any());
    }

    @Test
    void createTicket_blankIpAddress_bindsAsNullAndPassesValidation() throws Exception {
        TicketResponse response = TicketResponse.builder().id(7L).build();
        when(ticketService.createTicket(any(TicketCreateRequest.class), eq(currentUser))).thenReturn(response);

        mockMvc.perform(post("/tickets")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("assignedUserId", "1")
                        .param("description", "Printer is on fire")
                        .param("callerName", "Jane Doe")
                        .param("phoneNumber", "6912345678")
                        .param("departmentId", "1")
                        .param("categoryId", "1")
                        .param("priority", "HIGH")
                        .param("commentText", "Initial report")
                        .param("ipAddress", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=7"));

        verify(ticketService).createTicket(any(TicketCreateRequest.class), eq(currentUser));
    }

    @Test
    void createTicket_withoutAuthentication_redirectsToLoginAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/tickets")
                        .with(csrf())
                        .param("assignedUserId", "1")
                        .param("description", "Printer is on fire")
                        .param("callerName", "Jane Doe")
                        .param("phoneNumber", "6912345678")
                        .param("departmentId", "1")
                        .param("categoryId", "1")
                        .param("priority", "HIGH")
                        .param("commentText", "Initial report"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(ticketService);
    }

    @Test
    void createTicket_withoutCsrfToken_isForbiddenAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/tickets")
                        .with(user(currentUser))
                        .param("assignedUserId", "1")
                        .param("description", "Printer is on fire")
                        .param("callerName", "Jane Doe")
                        .param("phoneNumber", "6912345678")
                        .param("departmentId", "1")
                        .param("categoryId", "1")
                        .param("priority", "HIGH")
                        .param("commentText", "Initial report"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(ticketService);
    }

    @Test
    void resolveTicket_withComment_resolvesAndRedirectsToList() throws Exception {
        when(ticketService.resolveTicket(eq(5L), any(TicketChangeStatusRequest.class), eq(currentUser)))
                .thenReturn(TicketResponse.builder().id(5L).build());

        mockMvc.perform(post("/tickets/5/resolve")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", "Replaced the toner cartridge"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        verify(ticketService).resolveTicket(eq(5L), any(TicketChangeStatusRequest.class), eq(currentUser));
    }

    @Test
    void resolveTicket_withBlankComment_redirectsBackToTicketWithoutResolving() throws Exception {
        mockMvc.perform(post("/tickets/5/resolve")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=5"));

        verify(ticketService, never()).resolveTicket(any(), any(), any());
    }

    @Test
    void cancelTicket_withComment_cancelsAndRedirectsToList() throws Exception {
        when(ticketService.cancelTicket(eq(5L), any(TicketChangeStatusRequest.class), eq(currentUser)))
                .thenReturn(TicketResponse.builder().id(5L).build());

        mockMvc.perform(post("/tickets/5/cancel")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", "Duplicate ticket"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        verify(ticketService).cancelTicket(eq(5L), any(TicketChangeStatusRequest.class), eq(currentUser));
    }

    @Test
    void cancelTicket_withBlankComment_redirectsBackToTicketWithoutCancelling() throws Exception {
        mockMvc.perform(post("/tickets/5/cancel")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=5"));

        verify(ticketService, never()).cancelTicket(any(), any(), any());
    }

    @Test
    void reopenTicket_withComment_reopensAndRedirectsToList() throws Exception {
        when(ticketService.reopenTicket(eq(5L), any(TicketChangeStatusRequest.class), eq(currentUser)))
                .thenReturn(TicketResponse.builder().id(5L).build());

        mockMvc.perform(post("/tickets/5/reopen")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", "Issue came back"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        verify(ticketService).reopenTicket(eq(5L), any(TicketChangeStatusRequest.class), eq(currentUser));
    }

    @Test
    void reopenTicket_withBlankComment_redirectsBackToTicketWithoutReopening() throws Exception {
        mockMvc.perform(post("/tickets/5/reopen")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=5"));

        verify(ticketService, never()).reopenTicket(any(), any(), any());
    }

    @Test
    void commentOnTicket_withComment_addsCommentAndRedirectsToList() throws Exception {
        when(ticketService.commentOnTicket(eq(5L), any(TicketChangeStatusRequest.class), eq(currentUser)))
                .thenReturn(TicketResponse.builder().id(5L).build());

        mockMvc.perform(post("/tickets/5/comments")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", "Following up with the vendor"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        verify(ticketService).commentOnTicket(eq(5L), any(TicketChangeStatusRequest.class), eq(currentUser));
    }

    @Test
    void commentOnTicket_withBlankComment_redirectsBackToTicketWithoutCommenting() throws Exception {
        mockMvc.perform(post("/tickets/5/comments")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=5"));

        verify(ticketService, never()).commentOnTicket(any(), any(), any());
    }

    @Test
    void reassignTicket_withValidData_reassignsAndRedirectsToList() throws Exception {
        when(ticketService.reassignTicket(eq(5L), any(TicketReassignRequest.class), eq(currentUser)))
                .thenReturn(TicketResponse.builder().id(5L).build());

        mockMvc.perform(post("/tickets/5/reassign")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("assignedTo", "2")
                        .param("commentText", "Reassigning to network team"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        verify(ticketService).reassignTicket(eq(5L), any(TicketReassignRequest.class), eq(currentUser));
    }

    @Test
    void reassignTicket_missingAssignedTo_redirectsBackToTicketWithoutReassigning() throws Exception {
        mockMvc.perform(post("/tickets/5/reassign")
                        // assignedTo intentionally omitted
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", "Reassigning to network team"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=5"));

        verify(ticketService, never()).reassignTicket(any(), any(), any());
    }

    @Test
    void reassignTicket_withBlankComment_redirectsBackToTicketWithoutReassigning() throws Exception {
        mockMvc.perform(post("/tickets/5/reassign")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("assignedTo", "2")
                        .param("commentText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=5"));

        verify(ticketService, never()).reassignTicket(any(), any(), any());
    }

    @Test
    void deleteTicket_asAdmin_deletesAndRedirectsToList() throws Exception {
        mockMvc.perform(post("/tickets/5/delete")
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets"));

        verify(ticketService).deleteTicket(5L);
    }

    @Test
    void deleteTicket_asNonAdmin_isForbiddenAndDoesNotDelete() throws Exception {
        User regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("Chrianto");
        regularUser.setEmail("Chrianto@ece.auth.gr");
        regularUser.setRole(UserRole.USER);

        mockMvc.perform(post("/tickets/5/delete")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(ticketService, never()).deleteTicket(any());
    }

    @Test
    void viewTicket_redirectsToListWithOpenTicketParam() throws Exception {
        mockMvc.perform(get("/tickets/5")
                        .with(user(currentUser)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tickets?openTicket=5"));

        verifyNoInteractions(ticketService);
    }
}
