package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.DepartmentCreateRequest;
import com.Chrianto.TicketingSystem.dto.response.DepartmentResponse;
import com.Chrianto.TicketingSystem.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PostMapping("/{departmentId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editDepartment(@PathVariable Long departmentId, @RequestParam String name) {
        departmentService.updateDepartmentName(departmentId, name);
        return "redirect:/departments";
    }

    @PostMapping("/{departmentId}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleDepartmentActive(@PathVariable Long departmentId) {
        departmentService.toggleDepartmentActiveState(departmentId);
        return "redirect:/departments";
    }

    private void populateListModel(Model model) {
        List<DepartmentResponse> allDepartments = departmentService.getAllDepartments();
        model.addAttribute("activeDepartments", allDepartments.stream().filter(DepartmentResponse::isActive).toList());
        model.addAttribute("inactiveDepartments", allDepartments.stream().filter(d -> !d.isActive()).toList());
        if (!model.containsAttribute("departmentCreateRequest")) {
            model.addAttribute("departmentCreateRequest", new DepartmentCreateRequest());
        }
    }
}
