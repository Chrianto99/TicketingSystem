package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.CategoryRequest;
import com.Chrianto.TicketingSystem.dto.request.SubcategoryRequest;
import com.Chrianto.TicketingSystem.dto.response.CategoryResponse;
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

    @PostMapping("/{categoryId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editCategory(@PathVariable Long categoryId, @RequestParam String name) {
        categoryService.updateCategoryName(categoryId, name);
        return "redirect:/categories";
    }

    @PostMapping("/{categoryId}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleCategoryActive(@PathVariable Long categoryId) {
        categoryService.toggleCategoryActiveState(categoryId);
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

    @PostMapping("/{categoryId}/subcategories/{subcategoryId}/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public String editSubcategory(@PathVariable Long categoryId, @PathVariable Long subcategoryId, @RequestParam String name) {
        subcategoryService.updateSubcategoryName(subcategoryId, name);
        return "redirect:/categories";
    }

    @PostMapping("/{categoryId}/subcategories/{subcategoryId}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public String toggleSubcategoryActive(@PathVariable Long categoryId, @PathVariable Long subcategoryId) {
        subcategoryService.toggleSubcategoryActiveState(subcategoryId);
        return "redirect:/categories";
    }

    private void populateListModel(Model model) {
        List<SubcategoryResponse> allSubcategories = subcategoryService.getAllSubcategories(null);
        Map<Long, List<SubcategoryResponse>> activeSubcategoriesByCategory = allSubcategories.stream()
                .filter(SubcategoryResponse::isActive)
                .collect(Collectors.groupingBy(SubcategoryResponse::getCategoryId));
        Map<Long, List<SubcategoryResponse>> inactiveSubcategoriesByCategory = allSubcategories.stream()
                .filter(s -> !s.isActive())
                .collect(Collectors.groupingBy(SubcategoryResponse::getCategoryId));

        List<CategoryResponse> allCategories = categoryService.getAllCategories();

        model.addAttribute("activeCategories", allCategories.stream().filter(CategoryResponse::isActive).toList());
        model.addAttribute("inactiveCategories", allCategories.stream().filter(c -> !c.isActive()).toList());
        model.addAttribute("activeSubcategoriesByCategory", activeSubcategoriesByCategory);
        model.addAttribute("inactiveSubcategoriesByCategory", inactiveSubcategoriesByCategory);
        if (!model.containsAttribute("categoryRequest")) {
            model.addAttribute("categoryRequest", new CategoryRequest());
        }
    }
}
