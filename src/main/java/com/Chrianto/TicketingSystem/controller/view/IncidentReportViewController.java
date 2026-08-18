package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.IncidentCommentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentReportCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentReportUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentTicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.IncidentReportResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.service.AttachmentService;
import com.Chrianto.TicketingSystem.service.IncidentReportService;
import com.Chrianto.TicketingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentReportViewController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final IncidentReportService incidentReportService;
    private final UserService userService;
    private final AttachmentService attachmentService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listIncidents(@RequestParam(required = false) IncidentStatus status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                                 Model model) {
        Page<IncidentReportResponse> incidentPage = incidentReportService.getAllIncidentReports(status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("incidents", incidentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", Math.max(1, incidentPage.getTotalPages()));
        model.addAttribute("pageSize", size);
        model.addAttribute("statusFilter", status);
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("priorities", TicketPriority.values());
        if (!model.containsAttribute("incidentReportCreateRequest")) {
            model.addAttribute("incidentReportCreateRequest", new IncidentReportCreateRequest());
        }
        return "incidents/list";
    }

    @PostMapping
    public String createIncident(@Valid @ModelAttribute("incidentReportCreateRequest") IncidentReportCreateRequest req,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal User currentUser) {
        if (!bindingResult.hasErrors()) {
            incidentReportService.createIncidentReport(req, currentUser);
        }
        return "redirect:/incidents";
    }

    @GetMapping("/{incidentId}")
    public String viewIncident(@PathVariable Long incidentId, Model model) {
        var incident = incidentReportService.getIncidentReportById(incidentId);

        model.addAttribute("incident", incident);
        model.addAttribute("comments", incidentReportService.getCommentsForIncident(incidentId));
        model.addAttribute("relatedTickets", incidentReportService.getTicketsForIncident(incidentId));
        model.addAttribute("attachments", attachmentService.getAttachmentsForIncident(incidentId));
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("priorities", TicketPriority.values());

        if (!model.containsAttribute("incidentReportUpdateRequest")) {
            IncidentReportUpdateRequest updateReq = new IncidentReportUpdateRequest();
            updateReq.setSubject(incident.getSubject());
            updateReq.setDescription(incident.getDescription());
            updateReq.setPriority(incident.getPriority());
            model.addAttribute("incidentReportUpdateRequest", updateReq);
        }
        if (!model.containsAttribute("incidentCommentCreateRequest")) {
            model.addAttribute("incidentCommentCreateRequest", new IncidentCommentCreateRequest());
        }
        if (!model.containsAttribute("incidentTicketCreateRequest")) {
            model.addAttribute("incidentTicketCreateRequest", new IncidentTicketCreateRequest());
        }
        return "incidents/detail";
    }

    @PostMapping("/{incidentId}/edit")
    public String editIncident(@PathVariable Long incidentId,
                                @Valid @ModelAttribute("incidentReportUpdateRequest") IncidentReportUpdateRequest req,
                                BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            incidentReportService.editIncidentReport(incidentId, req);
        }
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/close")
    public String closeIncident(@PathVariable Long incidentId) {
        incidentReportService.closeIncident(incidentId);
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/reopen")
    public String reopenIncident(@PathVariable Long incidentId) {
        incidentReportService.reopenIncident(incidentId);
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/comments")
    public String addComment(@PathVariable Long incidentId,
                              @Valid @ModelAttribute("incidentCommentCreateRequest") IncidentCommentCreateRequest req,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal User currentUser) {
        if (!bindingResult.hasErrors()) {
            incidentReportService.addComment(incidentId, req, currentUser);
        }
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/tickets")
    public String createTicketFromIncident(@PathVariable Long incidentId,
                                            @Valid @ModelAttribute("incidentTicketCreateRequest") IncidentTicketCreateRequest req,
                                            BindingResult bindingResult,
                                            @AuthenticationPrincipal User currentUser) {
        if (!bindingResult.hasErrors()) {
            incidentReportService.createTicketFromIncident(incidentId, req, currentUser);
        }
        return "redirect:/incidents/" + incidentId;
    }
}
