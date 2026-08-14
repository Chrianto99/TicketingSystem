package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.CommentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketChangeStatusRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketReassignRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketUpdateRequest;
import com.Chrianto.TicketingSystem.dto.response.CategoryResponse;
import com.Chrianto.TicketingSystem.dto.response.DepartmentResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.service.CategoryService;
import com.Chrianto.TicketingSystem.service.DepartmentService;
import com.Chrianto.TicketingSystem.service.NotificationService;
import com.Chrianto.TicketingSystem.service.TicketService;
import com.Chrianto.TicketingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketViewController {

    private final TicketService ticketService;
    private final DepartmentService departmentService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final NotificationService notificationService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // blank optional text fields (e.g. ipAddress) should bind as null, not "",
        // since HTML forms always submit a value for present-but-empty inputs
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listTickets(@RequestParam(required = false) String status,
                               @RequestParam(required = false) TicketPriority priority,
                               @RequestParam(required = false) String scope,
                               @RequestParam(required = false) String query,
                               @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        populateListData(model, resolveStatusFilter(status), priority, resolveScopeFilter(scope), query, currentUser, pageable);
        populateCreateFormData(model, currentUser);
        return "tickets/list";
    }

    @PostMapping
    public String createTicket(@Valid @ModelAttribute("ticketCreateRequest") TicketCreateRequest req,
                                BindingResult bindingResult,
                                @RequestParam(defaultValue = "open") String action,
                                @RequestParam(required = false) String status,
                                @RequestParam(required = false) TicketPriority priority,
                                @RequestParam(required = false) String scope,
                                @RequestParam(required = false) String query,
                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                @AuthenticationPrincipal User currentUser,
                                Model model) {
        if (req.getResolution() != null && !req.getResolution().isBlank() && req.getSubcategoryId() == null) {
            bindingResult.rejectValue("subcategoryId", "resolution.requiresSubcategory",
                    "Απαιτείται υποκατηγορία για την επίλυση ενός ticket");
        }
        if (bindingResult.hasErrors()) {
            populateListData(model, resolveStatusFilter(status), priority, resolveScopeFilter(scope), query, currentUser, pageable);
            populateCreateFormData(model, currentUser);
            return "tickets/list";
        }
        ticketService.createTicket(req, currentUser);
        return "redirect:/tickets";
    }

    @GetMapping("/{ticketId}")
    public String viewTicket(@PathVariable Long ticketId) {
        return "redirect:/tickets?openTicket=" + ticketId;
    }

    @GetMapping("/{ticketId}/edit")
    public String editTicketForm(@PathVariable Long ticketId, Model model) {
        model.addAttribute("ticket", ticketService.getTicketById(ticketId));
        model.addAttribute("ticketUpdateRequest", new TicketUpdateRequest());
        return "tickets/edit";
    }

    @PostMapping("/{ticketId}/edit")
    public String editTicket(@PathVariable Long ticketId,
                              @Valid @ModelAttribute("ticketUpdateRequest") TicketUpdateRequest req,
                              BindingResult bindingResult,
                              @AuthenticationPrincipal User currentUser,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/tickets?openTicket=" + ticketId;
        }
        ticketService.editTicket(ticketId, req, currentUser);
        return "redirect:/tickets?openTicket=" + ticketId;
    }

    @PostMapping("/{ticketId}/resolve")
    public String resolveTicket(@PathVariable Long ticketId,
                                 @Valid @ModelAttribute("commentRequest") TicketChangeStatusRequest req,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal User currentUser,
                                 RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/tickets?openTicket=" + ticketId;
        }
        ticketService.resolveTicket(ticketId, req, currentUser);
        return "redirect:/tickets";
    }

    @PostMapping("/{ticketId}/cancel")
    public String cancelTicket(@PathVariable Long ticketId,
                                @Valid @ModelAttribute("commentRequest") TicketChangeStatusRequest req,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/tickets?openTicket=" + ticketId;
        }
        ticketService.cancelTicket(ticketId, req, currentUser);
        return "redirect:/tickets";
    }

    @PostMapping("/{ticketId}/reopen")
    public String reopenTicket(@PathVariable Long ticketId,
                                @Valid @ModelAttribute("commentRequest") TicketChangeStatusRequest req,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal User currentUser,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/tickets?openTicket=" + ticketId;
        }
        ticketService.reopenTicket(ticketId, req, currentUser);
        return "redirect:/tickets";
    }

    @PostMapping("/{ticketId}/comments")
    public String commentOnTicket(@PathVariable Long ticketId,
                                   @Valid @ModelAttribute("commentRequest") TicketChangeStatusRequest req,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal User currentUser,
                                   RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/tickets?openTicket=" + ticketId;
        }
        ticketService.commentOnTicket(ticketId, req, currentUser);
        return "redirect:/tickets";
    }

    @PostMapping("/{ticketId}/comments/{commentId}/edit")
    public String editComment(@PathVariable Long ticketId,
                               @PathVariable Long commentId,
                               @Valid @ModelAttribute("commentUpdateRequest") CommentUpdateRequest req,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User currentUser,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/tickets?openTicket=" + ticketId;
        }
        ticketService.editComment(commentId, req, currentUser);
        return "redirect:/tickets?openTicket=" + ticketId;
    }

    @PostMapping("/{ticketId}/reassign")
    public String reassignTicket(@PathVariable Long ticketId,
                                  @Valid @ModelAttribute("reassignRequest") TicketReassignRequest req,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal User currentUser,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            flashValidationErrors(bindingResult, redirectAttributes);
            return "redirect:/tickets?openTicket=" + ticketId;
        }
        ticketService.reassignTicket(ticketId, req, currentUser);
        return "redirect:/tickets";
    }

    @PostMapping("/{ticketId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteTicket(@PathVariable Long ticketId) {
        ticketService.deleteTicket(ticketId);
        return "redirect:/tickets";
    }

    private void flashValidationErrors(BindingResult bindingResult, RedirectAttributes redirectAttributes) {
        String message = bindingResult.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        redirectAttributes.addFlashAttribute("errorMessage", message);
    }

    private void populateListData(Model model, TicketStatus status, TicketPriority priority,
                                   String scope, String query, User currentUser, Pageable pageable) {
        Long assignedToUserId = "assigned".equals(scope) ? currentUser.getId() : null;
        if ("assigned".equals(scope)) {
            notificationService.markSeen(currentUser.getId());
        }
        model.addAttribute("hasUnseenAssignedTickets", notificationService.hasUnseenAssignedTickets(currentUser.getId()));
        model.addAttribute("ticketPage", ticketService.getAllTickets(status, priority,
                null, assignedToUserId, query, pageable));
        model.addAttribute("query", query);
        model.addAttribute("status", status);
        // status/scope are null here when the filter means "show everything" (needed for the
        // JPA query), but a null query param is dropped entirely by Thymeleaf's @{} link
        // builder — so pagination links must round-trip these string forms instead, or a
        // "next page" click silently resets both filters back to their defaults.
        model.addAttribute("statusParam", status == null ? "ALL" : status.name());
        model.addAttribute("statusFilterActive", status != TicketStatus.OPEN);
        model.addAttribute("priority", priority);
        model.addAttribute("scope", scope);
        model.addAttribute("scopeParam", scope == null ? "all" : scope);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", TicketPriority.values());
    }

    // No status param means the page was reached fresh (e.g. nav link) — default to OPEN.
    // An explicit "ALL" is how the filter dropdown asks to see every status.
    private TicketStatus resolveStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return TicketStatus.OPEN;
        }
        if ("ALL".equalsIgnoreCase(status)) {
            return null;
        }
        return TicketStatus.valueOf(status);
    }

    // No scope param means the page was reached fresh (e.g. nav link) — default to "assigned"
    // (My Tickets). An explicit "all" is how the All Tickets tab asks to see everyone's tickets.
    private String resolveScopeFilter(String scope) {
        if (scope == null || scope.isBlank()) {
            return "assigned";
        }
        if ("all".equalsIgnoreCase(scope)) {
            return null;
        }
        return scope;
    }

    private void populateCreateFormData(Model model, User currentUser) {
        model.addAttribute("departments", departmentService.getAllDepartments().stream().filter(DepartmentResponse::isActive).toList());
        model.addAttribute("categories", categoryService.getAllCategories().stream().filter(CategoryResponse::isActive).toList());
        model.addAttribute("users", userService.getAllUsers());
        if (!model.containsAttribute("ticketCreateRequest")) {
            TicketCreateRequest req = new TicketCreateRequest();
            req.setAssignedUserId(currentUser.getId());
            req.setPriority(TicketPriority.MEDIUM);
            model.addAttribute("ticketCreateRequest", req);
        }
    }
}
