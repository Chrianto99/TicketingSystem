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

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentViewController {

    private static final int DEFAULT_PAGE_SIZE = 15;

    private final DepartmentService departmentService;

    @GetMapping
    public String listDepartments(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size,
                                   Model model) {
        populateListModel(model, page, size);
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
    public String editDepartment(@PathVariable Long departmentId, @RequestParam String name,
                                  @RequestParam(required = false) String location) {
        departmentService.updateDepartment(departmentId, name, location);
        return "redirect:/departments";
    }

    @PostMapping("/{departmentId}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleDepartmentActive(@PathVariable Long departmentId) {
        departmentService.toggleDepartmentActiveState(departmentId);
        return "redirect:/departments";
    }

    private void populateListModel(Model model, int page, int size) {
        List<DepartmentResponse> allDepartments = departmentService.getAllDepartments();
        // Active departments first, then inactive — same order the page previously
        // rendered them in, just now sliced into pages instead of shown all at once.
        List<DepartmentResponse> ordered = new ArrayList<>(allDepartments.stream().filter(DepartmentResponse::isActive).toList());
        ordered.addAll(allDepartments.stream().filter(d -> !d.isActive()).toList());

        int totalItems = ordered.size();
        int totalPages = totalItems == 0 ? 1 : (int) Math.ceil(totalItems / (double) size);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int fromIndex = Math.min(safePage * size, totalItems);
        int toIndex = Math.min(fromIndex + size, totalItems);
        List<DepartmentResponse> pageContent = ordered.subList(fromIndex, toIndex);

        model.addAttribute("activeDepartments", pageContent.stream().filter(DepartmentResponse::isActive).toList());
        model.addAttribute("inactiveDepartments", pageContent.stream().filter(d -> !d.isActive()).toList());
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageSize", size);
        if (!model.containsAttribute("departmentCreateRequest")) {
            model.addAttribute("departmentCreateRequest", new DepartmentCreateRequest());
        }
    }
}
