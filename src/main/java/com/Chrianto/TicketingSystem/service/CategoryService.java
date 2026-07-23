package com.Chrianto.TicketingSystem.service;

import com.Chrianto.TicketingSystem.dto.request.CategoryRequest;
import com.Chrianto.TicketingSystem.dto.response.CategoryResponse;
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
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final TicketRepository ticketRepository;

    public CategoryResponse createCategory(CategoryRequest req){
        Category category = new Category();

        category.setName(req.getName());

        category = categoryRepository.save(category);

        return toResponse(category);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new EntityNotFoundException("Category not found with id: " + categoryId);
        }

        // subcategories always belong to exactly one category, so they can't be
        // nulled out like tickets are — they're deleted along with the category
        List<Subcategory> subcategories = subcategoryRepository.findByCategoryId(categoryId);
        for (Subcategory subcategory : subcategories) {
            ticketRepository.nullifySubcategory(subcategory.getId());
        }
        subcategoryRepository.deleteAll(subcategories);

        ticketRepository.nullifyCategory(categoryId);
        categoryRepository.deleteById(categoryId);
    }

    public List<CategoryResponse> getAllCategories(){
        return categoryRepository.findAll().
                stream().
                map(this::toResponse).
                toList();
    }

    private CategoryResponse toResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }


}
