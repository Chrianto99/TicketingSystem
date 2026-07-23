package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.SubcategoryRequest;
import com.Chrianto.TicketingSystem.dto.response.SubcategoryResponse;
import com.Chrianto.TicketingSystem.entity.Category;
import com.Chrianto.TicketingSystem.entity.Subcategory;
import com.Chrianto.TicketingSystem.exception.EntityNotFoundException;
import com.Chrianto.TicketingSystem.repository.CategoryRepository;
import com.Chrianto.TicketingSystem.repository.SubcategoryRepository;
import com.Chrianto.TicketingSystem.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubcategoryService {
    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;

    public SubcategoryResponse createSubcategory(SubcategoryRequest req){
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Category not found"));

        Subcategory subcategory = new Subcategory();
        subcategory.setName(req.getName());
        subcategory.setCategory(category);

        subcategory = subcategoryRepository.save(subcategory);

        return toResponse(subcategory);
    }

    @Transactional
    public void deleteSubcategory(Long subcategoryId) {
        if (!subcategoryRepository.existsById(subcategoryId)) {
            throw new EntityNotFoundException("Subcategory not found with id: " + subcategoryId);
        }

        ticketRepository.nullifySubcategory(subcategoryId);
        subcategoryRepository.deleteById(subcategoryId);
    }

    public List<SubcategoryResponse> getAllSubcategories(Long categoryId){
        List<Subcategory> subcategories = categoryId != null
                ? subcategoryRepository.findByCategoryId(categoryId)
                : subcategoryRepository.findAll();

        return subcategories.stream()
                .map(this::toResponse)
                .toList();
    }

    private SubcategoryResponse toResponse(Subcategory subcategory) {
        return SubcategoryResponse.builder()
                .id(subcategory.getId())
                .name(subcategory.getName())
                .categoryId(subcategory.getCategory().getId())
                .categoryName(subcategory.getCategory().getName())
                .build();
    }


}
