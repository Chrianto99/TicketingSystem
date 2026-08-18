package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.ChangePasswordRequest;
import com.Chrianto.TicketingSystem.dto.request.UserProfileUpdateRequest;
import com.Chrianto.TicketingSystem.dto.response.UserResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileViewController {

    private final UserService userService;

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // blank optional fields (email, phoneNumber, firstName, lastName) should bind as
        // null, not "" — an empty string would collide with other blank-email users under
        // the unique constraint.
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }

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
            UserResponse freshUser = userService.getUserById(currentUser.getId());
            model.addAttribute("user", freshUser);
            model.addAttribute("userProfileUpdateRequest", profileRequestFor(freshUser));
            return "users/profile";
        }
        userService.changePassword(currentUser.getId(), req);
        return "redirect:/profile?passwordChanged=true";
    }

    private void populateProfileData(Model model, User currentUser) {
        UserResponse freshUser = userService.getUserById(currentUser.getId());
        model.addAttribute("user", freshUser);
        if (!model.containsAttribute("userProfileUpdateRequest")) {
            model.addAttribute("userProfileUpdateRequest", profileRequestFor(freshUser));
        }
        if (!model.containsAttribute("changePasswordRequest")) {
            model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        }
    }

    private UserProfileUpdateRequest profileRequestFor(UserResponse user) {
        UserProfileUpdateRequest req = new UserProfileUpdateRequest();
        req.setEmail(user.getEmail());
        req.setPhoneNumber(user.getPhoneNumber());
        req.setFirstName(user.getFirstName());
        req.setLastName(user.getLastName());
        req.setSpecialization(user.getSpecialization());
        return req;
    }
}
