package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import com.Chrianto.TicketingSystem.security.SecurityConfig;
import com.Chrianto.TicketingSystem.service.AttachmentService;
import com.Chrianto.TicketingSystem.service.IncidentService;
import com.Chrianto.TicketingSystem.service.TicketHistoryService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Covers the Incident feature's view-controller layer only, mirroring
// TicketViewControllerTest's style (MockMvc slice test, mocked services, no
// real DB). GET /incidents/{id} is deliberately NOT covered here: it renders
// incidents/detail.html, which walks several nested collections (comments,
// history, related tickets, attachments) and status/priority-derived badges —
// getting every mocked field right without being able to click through the
// page in a browser risks a misleading failure unrelated to real behavior, so
// that page needs an eyeballed check instead of a render assertion here.
//
// On a validation failure every POST handler here redirects to the same place
// a success would (unlike TicketViewController, which re-renders some forms
// inline) — but it now flashes an "errorMessage" attribute first, the same
// mechanism TicketViewController uses, picked up by fragments/nav :: modals +
// common.js's showError() on the next page load. The "_withBlank..." tests
// below check both halves: the service is skipped, and the error is shown.
// Business-rule failures thrown from the service (e.g. "already closed",
// "only the author can edit this") aren't tested here — they're handled
// globally by ViewExceptionHandler, not by anything in this controller.
@WebMvcTest(IncidentViewController.class)
@Import(SecurityConfig.class)
class IncidentViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IncidentService incidentService;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AttachmentService attachmentService;
    @MockitoBean
    private TicketHistoryService ticketHistoryService;
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

        when(incidentService.getAllIncidents(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void listIncidents_withNoFilters_defaultsStatusToOpen() throws Exception {
        mockMvc.perform(get("/incidents")
                        .with(user(currentUser)))
                .andExpect(status().isOk())
                .andExpect(model().attribute("statusFilter", IncidentStatus.OPEN))
                .andExpect(model().attribute("statusParam", "OPEN"))
                .andExpect(model().attribute("statusFilterActive", false))
                .andExpect(model().attribute("priorityFilter", (Object) null))
                .andExpect(model().attributeExists("incidentCreateRequest"));
    }

    @Test
    void listIncidents_withStatusAll_showsEveryStatus() throws Exception {
        mockMvc.perform(get("/incidents")
                        .with(user(currentUser))
                        .param("status", "ALL"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("statusFilter", (Object) null))
                .andExpect(model().attribute("statusParam", "ALL"))
                .andExpect(model().attribute("statusFilterActive", true));

        verify(incidentService).getAllIncidents(eq(null), any(), any(), any());
    }

    @Test
    void listIncidents_withStatusAndPriorityFilters_exposesThemOnTheModel() throws Exception {
        mockMvc.perform(get("/incidents")
                        .with(user(currentUser))
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("query", "printer"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("statusFilter", IncidentStatus.OPEN))
                .andExpect(model().attribute("priorityFilter", TicketPriority.HIGH))
                .andExpect(model().attribute("query", "printer"));
    }

    @Test
    void createIncident_withValidData_createsAndRedirectsToList() throws Exception {
        mockMvc.perform(post("/incidents")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("subject", "Network outage")
                        .param("description", "Whole building is offline")
                        .param("priority", "HIGH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents"));

        verify(incidentService).createIncident(any(), eq(currentUser));
    }

    @Test
    void createIncident_withBlankDescription_doesNotCreateAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("subject", "Network outage")
                        .param("description", "")
                        .param("priority", "HIGH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).createIncident(any(), any());
    }

    @Test
    void createIncident_missingPriority_doesNotCreateAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("subject", "Network outage")
                        .param("description", "Whole building is offline")
                        // priority intentionally omitted
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).createIncident(any(), any());
    }

    @Test
    void createIncident_withoutAuthentication_redirectsToLoginAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/incidents")
                        .with(csrf())
                        .param("description", "Whole building is offline")
                        .param("priority", "HIGH"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(incidentService);
    }

    @Test
    void createIncident_withoutCsrfToken_isForbiddenAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/incidents")
                        .with(user(currentUser))
                        .param("description", "Whole building is offline")
                        .param("priority", "HIGH"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(incidentService);
    }

    @Test
    void editIncident_withValidData_editsAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/incidents/5/edit")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("subject", "Network outage")
                        .param("description", "Updated description")
                        .param("priority", "MEDIUM"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"));

        verify(incidentService).editIncident(eq(5L), any(), eq(currentUser));
    }

    @Test
    void editIncident_withBlankDescription_doesNotEditAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents/5/edit")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("subject", "Network outage")
                        .param("description", "")
                        .param("priority", "MEDIUM"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).editIncident(any(), any(), any());
    }

    @Test
    void closeIncident_withReport_closesAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/incidents/5/close")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", "Replaced the failed switch"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"));

        verify(incidentService).closeIncident(eq(5L), any(), eq(currentUser));
    }

    @Test
    void closeIncident_withBlankReport_doesNotCloseAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents/5/close")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).closeIncident(any(), any(), any());
    }

    @Test
    void reopenIncident_withReport_reopensAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/incidents/5/reopen")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", "Switch has failed again"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"));

        verify(incidentService).reopenIncident(eq(5L), any(), eq(currentUser));
    }

    @Test
    void reopenIncident_withBlankReport_doesNotReopenAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents/5/reopen")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).reopenIncident(any(), any(), any());
    }

    @Test
    void addComment_withValidData_addsAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/incidents/5/comments")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", "Vendor has been notified"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"));

        verify(incidentService).addComment(eq(5L), any(), eq(currentUser));
    }

    @Test
    void addComment_withBlankText_doesNotAddAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents/5/comments")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("commentText", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).addComment(any(), any(), any());
    }

    @Test
    void editComment_withValidData_editsAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/incidents/5/comments/9/edit")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("text", "Updated report text"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"));

        verify(incidentService).editComment(eq(9L), any(), eq(currentUser));
    }

    @Test
    void editComment_withBlankText_doesNotEditAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents/5/comments/9/edit")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("text", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).editComment(any(), any(), any());
    }

    @Test
    void deleteComment_deletesAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/incidents/5/comments/9/delete")
                        .with(user(currentUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"));

        verify(incidentService).deleteComment(9L, currentUser);
    }

    @Test
    void createTicketFromIncident_withValidData_createsAndRedirectsToDetail() throws Exception {
        mockMvc.perform(post("/incidents/5/tickets")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("assignedUserId", "2")
                        .param("title", "Replace switch in server room")
                        .param("description", "Follow-up hardware ticket"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"));

        verify(incidentService).createTicketFromIncident(eq(5L), any(), eq(currentUser));
    }

    @Test
    void createTicketFromIncident_missingAssignedUser_doesNotCreateAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents/5/tickets")
                        .with(user(currentUser))
                        .with(csrf())
                        // assignedUserId intentionally omitted
                        .param("title", "Replace switch in server room"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).createTicketFromIncident(any(), any(), any());
    }

    @Test
    void createTicketFromIncident_missingTitle_doesNotCreateAndFlashesError() throws Exception {
        mockMvc.perform(post("/incidents/5/tickets")
                        .with(user(currentUser))
                        .with(csrf())
                        .param("assignedUserId", "2")
                        // title intentionally omitted
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/incidents/5"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(incidentService, never()).createTicketFromIncident(any(), any(), any());
    }
}
