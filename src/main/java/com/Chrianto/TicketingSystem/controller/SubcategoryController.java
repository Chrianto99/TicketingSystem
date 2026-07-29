package com.Chrianto.TicketingSystem.controller;

import com.Chrianto.TicketingSystem.dto.request.SubcategoryRequest;
import com.Chrianto.TicketingSystem.dto.response.SubcategoryResponse;
import com.Chrianto.TicketingSystem.service.SubcategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/subcategories")
@RequiredArgsConstructor
public class SubcategoryController {

    private final SubcategoryService subcategoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubcategoryResponse> createSubcategory(@Valid @RequestBody SubcategoryRequest req){
        return ResponseEntity.status(HttpStatus.CREATED).body(subcategoryService.createSubcategory(req));

    }

    @GetMapping
    public ResponseEntity<List<SubcategoryResponse>> getAllSubcategories(@RequestParam(required = false) Long categoryId){
        return ResponseEntity.ok(subcategoryService.getAllSubcategories(categoryId));
    }

    @DeleteMapping("/{subcategoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSubcategory(@PathVariable Long subcategoryId) {
        subcategoryService.deleteSubcategory(subcategoryId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{subcategoryId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubcategoryResponse> updateSubcategory(@PathVariable Long subcategoryId,
                                                                   @Valid @RequestBody SubcategoryRequest req) {
        return ResponseEntity.ok(subcategoryService.updateSubcategoryName(subcategoryId, req.getName()));
    }

    @PatchMapping("/{subcategoryId}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleSubcategoryActive(@PathVariable Long subcategoryId) {
        subcategoryService.toggleSubcategoryActiveState(subcategoryId);
        return ResponseEntity.noContent().build();
    }
}
