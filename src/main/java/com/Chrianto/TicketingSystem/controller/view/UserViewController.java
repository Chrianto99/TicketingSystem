package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.UserRegisterRequest;
import com.Chrianto.TicketingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserViewController {

    private final UserService userService;

    @GetMapping
    public String listUsers(Model model) {
        model.addAttribute("users", userService.getAllUsers());
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
        return "users/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@Valid @ModelAttribute("userRegisterRequest") UserRegisterRequest req,
                              BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "users/form";
        }
        userService.registerUser(req);
        return "redirect:/users";
    }

    @PostMapping("/{userId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return "redirect:/users";
    }
}
