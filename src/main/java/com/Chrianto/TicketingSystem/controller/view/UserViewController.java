package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.UserRegisterRequest;
import com.Chrianto.TicketingSystem.dto.response.UserResponse;
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import com.Chrianto.TicketingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import com.Chrianto.TicketingSystem.entity.User;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserViewController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // blank optional fields (email, phoneNumber) should bind as null, not "" — an empty
        // string would collide with other blank-email users under the unique constraint.
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                             Model model) {
        List<UserResponse> allUsers = userService.getAllUsers();

        int totalItems = allUsers.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) size);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(safePage * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);

        model.addAttribute("users", allUsers.subList(fromIndex, toIndex));
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        return "users/list";
    }

    @GetMapping("/{userId}")
    public String viewUser(@PathVariable Long userId, Model model) {
        model.addAttribute("user", userService.getUserById(userId));
        return "users/detail";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newUserForm(Model model) {
        model.addAttribute("userRegisterRequest", new UserRegisterRequest());
        model.addAttribute("roles", UserRole.values());
        return "users/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@Valid @ModelAttribute("userRegisterRequest") UserRegisterRequest req,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("roles", UserRole.values());
            return "users/form";
        }
        userService.registerUser(req);
        return "redirect:/users?registered=true";
    }

    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    public String resetPassword(@PathVariable Long userId) {
        userService.resetPassword(userId);
        return "redirect:/users?passwordReset=true";
    }

    @PostMapping("/{userId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return "redirect:/users?deactivated=true";
    }

    @PostMapping("/{userId}/reactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public String reactivateUser(@PathVariable Long userId) {
        userService.reactivateUser(userId);
        return "redirect:/users?reactivated=true";
    }

    @PostMapping("/{userId}/toggle-admin")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleAdmin(@PathVariable Long userId, @AuthenticationPrincipal User currentUser) {
        if (userId.equals(currentUser.getId())) {
            return "redirect:/users?roleChangeError=true";
        }
        userService.toggleAdminRole(userId);
        return "redirect:/users?roleChanged=true";
    }
}
