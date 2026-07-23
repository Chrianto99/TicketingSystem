package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.CategoryRequest;
import com.Chrianto.TicketingSystem.dto.request.SubcategoryRequest;
import com.Chrianto.TicketingSystem.dto.response.SubcategoryResponse;
import com.Chrianto.TicketingSystem.service.CategoryService;
import com.Chrianto.TicketingSystem.service.SubcategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryViewController {

    private final CategoryService categoryService;
    private final SubcategoryService subcategoryService;

    @GetMapping
    public String listCategories(Model model) {
        populateListModel(model);
        return "categories/list";
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public String createCategory(@Valid @ModelAttribute("categoryRequest") CategoryRequest req,
                                  BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            categoryService.createCategory(req);
        }
        return "redirect:/categories";
    }

    @PostMapping("/{categoryId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return "redirect:/categories";
    }

    @PostMapping("/{categoryId}/subcategories")
    @PreAuthorize("hasRole('ADMIN')")
    public String createSubcategory(@PathVariable Long categoryId, @RequestParam String name) {
        SubcategoryRequest req = new SubcategoryRequest();
        req.setName(name);
        req.setCategoryId(categoryId);
        subcategoryService.createSubcategory(req);
        return "redirect:/categories";
    }

    @PostMapping("/{categoryId}/subcategories/{subcategoryId}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteSubcategory(@PathVariable Long categoryId, @PathVariable Long subcategoryId) {
        subcategoryService.deleteSubcategory(subcategoryId);
        return "redirect:/categories";
    }

    private void populateListModel(Model model) {
        List<SubcategoryResponse> allSubcategories = subcategoryService.getAllSubcategories(null);
        Map<Long, List<SubcategoryResponse>> subcategoriesByCategory = allSubcategories.stream()
                .collect(Collectors.groupingBy(SubcategoryResponse::getCategoryId));

        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("subcategoriesByCategory", subcategoriesByCategory);
        if (!model.containsAttribute("categoryRequest")) {
            model.addAttribute("categoryRequest", new CategoryRequest());
        }
    }
}
