package com.Chrianto.TicketingSystem.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// Handles errors thrown by the Thymeleaf view controllers: redirects back to the
// page the request came from with a flash error message instead of rendering JSON.
@ControllerAdvice(basePackages = "com.Chrianto.TicketingSystem.controller.view")
public class ViewExceptionHandler {

    @ExceptionHandler({EntityNotFoundException.class, IllegalStateException.class, IllegalArgumentException.class})
    public String handleError(RuntimeException ex, HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:" + refererOrFallback(request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrityViolation(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Αυτή η καταχώρηση έρχεται σε σύγκρουση με μια υπάρχουσα εγγραφή");
        return "redirect:" + refererOrFallback(request);
    }

    private String refererOrFallback(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        return referer != null ? referer : "/tickets";
    }
}
