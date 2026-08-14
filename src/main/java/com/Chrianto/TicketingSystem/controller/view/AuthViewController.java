package com.Chrianto.TicketingSystem.controller.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthViewController {

    @GetMapping("/")
    public String root() {
        return "redirect:/tickets";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }
}
