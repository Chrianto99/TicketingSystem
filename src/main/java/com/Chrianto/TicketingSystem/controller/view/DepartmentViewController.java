package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.DepartmentCreateRequest;
import com.Chrianto.TicketingSystem.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentViewController {

    private final DepartmentService departmentService;

    @GetMapping
    public String listDepartments(Model model) {
        populateListModel(model);
        return "departments/list";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createDepartment(@Valid @ModelAttribute("departmentCreateRequest") DepartmentCreateRequest req,
                                    BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            departmentService.createDepartment(req);
        }
        return "redirect:/departments";
    }

    @PostMapping("/{departmentId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteDepartment(@PathVariable Long departmentId) {
        departmentService.deleteDepartment(departmentId);
        return "redirect:/departments";
    }

    private void populateListModel(Model model) {
        model.addAttribute("departments", departmentService.getAllDepartments());
        if (!model.containsAttribute("departmentCreateRequest")) {
            model.addAttribute("departmentCreateRequest", new DepartmentCreateRequest());
        }
    }
}
