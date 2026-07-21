package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.ProblemTypeRequest;
import com.Chrianto.TicketingSystem.service.ProblemTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/problemTypes")
@RequiredArgsConstructor
public class ProblemTypeViewController {

    private final ProblemTypeService problemTypeService;

    @GetMapping
    public String listProblemTypes(Model model) {
        model.addAttribute("problemTypes", problemTypeService.getAllProblemTypes());
        return "problemTypes/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasRole('ADMIN')")
    public String newProblemTypeForm(Model model) {
        model.addAttribute("problemTypeRequest", new ProblemTypeRequest());
        return "problemTypes/form";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createProblemType(@Valid @ModelAttribute("problemTypeRequest") ProblemTypeRequest req,
                                     BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "problemTypes/form";
        }
        problemTypeService.createProblemType(req);
        return "redirect:/problemTypes";
    }

    @PostMapping("/{problemTypeId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteProblemType(@PathVariable Long problemTypeId) {
        problemTypeService.deleteProblemType(problemTypeId);
        return "redirect:/problemTypes";
    }
}
