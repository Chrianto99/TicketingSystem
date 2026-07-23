package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.CommentUpdateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketChangeStatusRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketCreateRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketReassignRequest;
import com.Chrianto.TicketingSystem.dto.request.TicketUpdateRequest;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.TicketPriority;
import com.Chrianto.TicketingSystem.entity.enums.TicketStatus;
import com.Chrianto.TicketingSystem.service.CategoryService;
import com.Chrianto.TicketingSystem.service.DepartmentService;
import com.Chrianto.TicketingSystem.service.TicketHistoryService;
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
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketViewController {

    private final TicketService ticketService;
    private final TicketHistoryService ticketHistoryService;
    private final DepartmentService departmentService;
    private final CategoryService categoryService;
    private final UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // blank optional text fields (e.g. ipAddress) should bind as null, not "",
        // since HTML forms always submit a value for present-but-empty inputs
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listTickets(@RequestParam(required = false) TicketStatus status,
                               @RequestParam(required = false) TicketPriority priority,
                               @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                               @AuthenticationPrincipal User currentUser,
                               Model model) {
        populateListData(model, status, priority, pageable);
        populateCreateFormData(model, currentUser);
        return "tickets/list";
    }

    @PostMapping
    public String createTicket(@Valid @ModelAttribute("ticketCreateRequest") TicketCreateRequest req,
                                BindingResult bindingResult,
                                @RequestParam(defaultValue = "open") String action,
                                @RequestParam(required = false) TicketStatus status,
                                @RequestParam(required = false) TicketPriority priority,
                                @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                @AuthenticationPrincipal User currentUser,
                                Model model) {
        if (bindingResult.hasErrors()) {
            populateListData(model, status, priority, pageable);
            populateCreateFormData(model, currentUser);
            return "tickets/list";
        }
        // "action" distinguishes "Open Ticket" vs "Open and Resolve" — both currently just create
        // the ticket; the differentiated behavior for "open-resolve" is still to be defined.
        var created = ticketService.createTicket(req, currentUser);
        return "redirect:/tickets?openTicket=" + created.getId();
    }

    @GetMapping("/{ticketId}")
    public String viewTicket(@PathVariable Long ticketId, Model model) {
        populateTicketDetail(ticketId, model);
        return "tickets/detail";
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
                              Model model) {
        if (bindingResult.hasErrors()) {
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
                                 Model model) {
        if (bindingResult.hasErrors()) {
            populateTicketDetail(ticketId, model);
            return "tickets/detail";
        }
        ticketService.resolveTicket(ticketId, req, currentUser);
        return "redirect:/tickets";
    }

    @PostMapping("/{ticketId}/cancel")
    public String cancelTicket(@PathVariable Long ticketId,
                                @Valid @ModelAttribute("commentRequest") TicketChangeStatusRequest req,
                                BindingResult bindingResult,
                                @AuthenticationPrincipal User currentUser,
                                Model model) {
        if (bindingResult.hasErrors()) {
            populateTicketDetail(ticketId, model);
            return "tickets/detail";
        }
        ticketService.cancelTicket(ticketId, req, currentUser);
        return "redirect:/tickets";
    }

    @PostMapping("/{ticketId}/comments")
    public String commentOnTicket(@PathVariable Long ticketId,
                                   @Valid @ModelAttribute("commentRequest") TicketChangeStatusRequest req,
                                   BindingResult bindingResult,
                                   @AuthenticationPrincipal User currentUser,
                                   Model model) {
        if (bindingResult.hasErrors()) {
            populateTicketDetail(ticketId, model);
            return "tickets/detail";
        }
        ticketService.commentOnTicket(ticketId, req, currentUser);
        return "redirect:/tickets";
    }

    @PostMapping("/{ticketId}/comments/{commentId}/edit")
    public String editComment(@PathVariable Long ticketId,
                               @PathVariable Long commentId,
                               @Valid @ModelAttribute("commentUpdateRequest") CommentUpdateRequest req,
                               BindingResult bindingResult,
                               @AuthenticationPrincipal User currentUser) {
        if (bindingResult.hasErrors()) {
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
                                  Model model) {
        if (bindingResult.hasErrors()) {
            populateTicketDetail(ticketId, model);
            return "tickets/detail";
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

    private void populateTicketDetail(Long ticketId, Model model) {
        model.addAttribute("ticket", ticketService.getTicketById(ticketId));
        model.addAttribute("history", ticketHistoryService.getTicketHistory(ticketId));
        if (!model.containsAttribute("commentRequest")) {
            model.addAttribute("commentRequest", new TicketChangeStatusRequest());
        }
        if (!model.containsAttribute("reassignRequest")) {
            model.addAttribute("reassignRequest", new TicketReassignRequest());
        }
    }

    private void populateListData(Model model, TicketStatus status, TicketPriority priority, Pageable pageable) {
        model.addAttribute("ticketPage", ticketService.getAllTickets(status, priority, pageable));
        model.addAttribute("status", status);
        model.addAttribute("priority", priority);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("priorities", TicketPriority.values());
    }

    private void populateCreateFormData(Model model, User currentUser) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("users", userService.getAllUsers());
        if (!model.containsAttribute("ticketCreateRequest")) {
            TicketCreateRequest req = new TicketCreateRequest();
            req.setAssignedUserId(currentUser.getId());
            req.setPriority(TicketPriority.MEDIUM);
            model.addAttribute("ticketCreateRequest", req);
        }
    }
}
