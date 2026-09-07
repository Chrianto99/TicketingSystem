package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.CommentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentCommentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.IncidentTicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.IncidentResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.IncidentStatus;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.service.AttachmentService;
import com.Chrianto.TicketingSystem.service.IncidentService;
import com.Chrianto.TicketingSystem.service.TicketHistoryService;
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
import org.springframework.validation.FieldError;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;

@Controller
@RequestMapping("/incidents")
@RequiredArgsConstructor
public class IncidentViewController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final IncidentService incidentService;
    private final UserService userService;
    private final AttachmentService attachmentService;
    private final TicketHistoryService ticketHistoryService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listIncidents(@RequestParam(required = false) String status,
                                 @RequestParam(required = false) TicketPriority priority,
                                 @RequestParam(required = false) String query,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                                 Model model) {
        IncidentStatus resolvedStatus = resolveStatusFilter(status);
        Page<IncidentResponse> incidentPage = incidentService.getAllIncidents(resolvedStatus, priority, query,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        model.addAttribute("incidents", incidentPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", Math.max(1, incidentPage.getTotalPages()));
        model.addAttribute("pageSize", size);
        model.addAttribute("statusFilter", resolvedStatus);
        // resolvedStatus is null for "ALL", which a Thymeleaf @{} link drops
        // entirely instead of round-tripping — statusParam carries the literal
        // choice across pagination links instead.
        model.addAttribute("statusParam", resolvedStatus == null ? "ALL" : resolvedStatus.name());
        model.addAttribute("statusFilterActive", resolvedStatus != IncidentStatus.OPEN);
        model.addAttribute("priorityFilter", priority);
        model.addAttribute("query", query);
        model.addAttribute("statuses", IncidentStatus.values());
        model.addAttribute("priorities", TicketPriority.values());
        if (!model.containsAttribute("incidentCreateRequest")) {
            model.addAttribute("incidentCreateRequest", new IncidentCreateRequest());
        }
        return "incidents/list";
    }

    // No status param means the page was reached fresh (e.g. nav link) —
    // default to OPEN, same convention as the Tickets page. The literal "ALL"
    // sentinel (from the status <select>) means show every status.
    private IncidentStatus resolveStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return IncidentStatus.OPEN;
        }
        if ("ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return IncidentStatus.valueOf(status);
    }

    @PostMapping
    public String createIncident(@Valid @ModelAttribute("incidentCreateRequest") IncidentCreateRequest req,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal User currentUser,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/incidents";
        }
        incidentService.createIncident(req, currentUser);
        return "redirect:/incidents";
    }

    @GetMapping("/{incidentId}")
    public String viewIncident(@PathVariable Long incidentId, Model model) {
        var incident = incidentService.getIncidentById(incidentId);

        model.addAttribute("incident", incident);
        model.addAttribute("comments", incidentService.getCommentsForIncident(incidentId));
        model.addAttribute("relatedTickets", incidentService.getTicketsForIncident(incidentId));
        model.addAttribute("attachments", attachmentService.getAttachmentsForIncident(incidentId));
        model.addAttribute("history", ticketHistoryService.getIncidentHistory(incidentId));
        model.addAttribute("users", userService.getAllActiveUsers());
        model.addAttribute("priorities", TicketPriority.values());

        if (!model.containsAttribute("incidentUpdateRequest")) {
            IncidentUpdateRequest updateReq = new IncidentUpdateRequest();
            updateReq.setSubject(incident.getSubject());
            updateReq.setDescription(incident.getDescription());
            updateReq.setPriority(incident.getPriority());
            model.addAttribute("incidentUpdateRequest", updateReq);
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
                                @Valid @ModelAttribute("incidentUpdateRequest") IncidentUpdateRequest req,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/incidents/" + incidentId;
        }
        incidentService.editIncident(incidentId, req, currentUser);
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/close")
    public String closeIncident(@PathVariable Long incidentId,
                                 @Valid @ModelAttribute("incidentCommentCreateRequest") IncidentCommentCreateRequest req,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/incidents/" + incidentId;
        }
        incidentService.closeIncident(incidentId, req, currentUser);
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/reopen")
    public String reopenIncident(@PathVariable Long incidentId,
                                  @Valid @ModelAttribute("incidentCommentCreateRequest") IncidentCommentCreateRequest req,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal User currentUser,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/incidents/" + incidentId;
        }
        incidentService.reopenIncident(incidentId, req, currentUser);
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/comments")
    public String addComment(@PathVariable Long incidentId,
                              @Valid @ModelAttribute("incidentCommentCreateRequest") IncidentCommentCreateRequest req,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/incidents/" + incidentId;
        }
        incidentService.addComment(incidentId, req, currentUser);
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/comments/{commentId}/edit")
    public String editComment(@PathVariable Long incidentId, @PathVariable Long commentId,
                               @Valid @ModelAttribute CommentUpdateRequest req,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User currentUser,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/incidents/" + incidentId;
        }
        incidentService.editComment(commentId, req, currentUser);
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long incidentId, @PathVariable Long commentId,
                                 @AuthenticationPrincipal User currentUser) {
        incidentService.deleteComment(commentId, currentUser);
        return "redirect:/incidents/" + incidentId;
    }

    @PostMapping("/{incidentId}/tickets")
    public String createTicketFromIncident(@PathVariable Long incidentId,
                                            @Valid @ModelAttribute("incidentTicketCreateRequest") IncidentTicketCreateRequest req,
                                            BindingResult bindingResult,
                                            @AuthenticationPrincipal User currentUser,
                                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/incidents/" + incidentId;
        }
        incidentService.createTicketFromIncident(incidentId, req, currentUser);
        return "redirect:/incidents/" + incidentId;
    }

    private void flashValidationErrors(BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        String message = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        redirectAttributes.addFlashAttribute("errorMessage", message);
    }
}
