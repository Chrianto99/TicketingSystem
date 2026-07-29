package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.ChangePasswordRequest;
import com.Chrianto.TicketingSystem.dto.request.UserProfileUpdateRequest;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileViewController {

    private final UserService userService;

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal User currentUser, Model model) {
        populateProfileData(model, currentUser);
        return "users/profile";
    }

    @PostMapping
    public String updateProfile(@Valid @ModelAttribute("userProfileUpdateRequest") UserProfileUpdateRequest req,
                                 BindingResult bindingResult,
                                 @AuthenticationPrincipal User currentUser,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getUserById(currentUser.getId()));
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
            return "users/profile";
        }
        userService.updateProfile(currentUser.getId(), req);
        return "redirect:/profile?updated=true";
    }

    @PostMapping("/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordRequest") ChangePasswordRequest req,
                                  BindingResult bindingResult,
                                  @AuthenticationPrincipal User currentUser,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("user", userService.getUserById(currentUser.getId()));
            model.addAttribute("userProfileUpdateRequest", profileRequestFor(currentUser));
            return "users/profile";
        }
        userService.changePassword(currentUser.getId(), req);
        return "redirect:/profile?passwordChanged=true";
    }

    private void populateProfileData(Model model, User currentUser) {
        model.addAttribute("user", userService.getUserById(currentUser.getId()));
        if (!model.containsAttribute("userProfileUpdateRequest")) {
            model.addAttribute("userProfileUpdateRequest", profileRequestFor(currentUser));
        }
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
    }

    private UserProfileUpdateRequest profileRequestFor(User currentUser) {
        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
        req.setEmail(currentUser.getEmail());
        return req;
    }
}
