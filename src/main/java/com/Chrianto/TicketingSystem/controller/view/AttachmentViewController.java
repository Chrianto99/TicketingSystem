package com.Chrianto.TicketingSystem.controller.view;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

// Admin-only "Διαχείριση" sub-tab hosting the old-attachment cleanup panel
// (previously embedded at the bottom of the Users page). The actual preview/
// cleanup calls go straight to the existing AttachmentController REST
// endpoints from attachments.js — this controller only serves the page shell.
@Controller
@RequestMapping("/attachments")
@PreAuthorize("hasRole('ADMIN')")
public class AttachmentViewController {

    @GetMapping("/cleanup")
    public String cleanup() {
        return "attachments/cleanup";
    }
}
