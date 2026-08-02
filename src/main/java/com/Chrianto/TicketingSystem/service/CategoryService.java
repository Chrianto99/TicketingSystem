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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final SubcategoryRepository subcategoryRepository;
    private final TicketRepository ticketRepository;

    public CategoryResponse createCategory(CategoryRequest req){
        Category category = new Category();

        category.setName(req.getName());
        category.setActive(true);

        category = categoryRepository.save(category);

        return toResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategoryName(Long categoryId, String name) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε κατηγορία βλάβης με id: " + categoryId));

        category.setName(name);
        category = categoryRepository.save(category);

        return toResponse(category);
    }

    @Transactional
    public void toggleCategoryActiveState(Long categoryId) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε κατηγορία βλάβης με id: " + categoryId));

        boolean newState = !category.isActive();
        category.setActive(newState);
        categoryRepository.save(category);

        if (!newState) {
            // only cascade on deactivation — reactivating the category
            // does NOT automatically reactivate its subcategories
            List<Subcategory> subcategories = subcategoryRepository.findByCategoryId(categoryId);
            for (Subcategory s : subcategories) {
                s.setActive(false);
            }
            subcategoryRepository.saveAll(subcategories);
        }
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε κατηγορία βλάβης με id: " + categoryId));

        if (category.isActive()) {
            throw new IllegalStateException("Μόνο απενεργοποιημένες κατηγορίες βλάβης μπορούν να διαγραφούν");
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
        Map<Long, Long> ticketCounts = ticketRepository.countTicketsGroupedByCategory().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        return categoryRepository.findAll().stream()
                .map(c -> toResponse(c, ticketCounts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    private CategoryResponse toResponse(Category category) {
        return toResponse(category, ticketRepository.countByCategoryId(category.getId()));
    }

    private CategoryResponse toResponse(Category category, long ticketCount) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .active(category.isActive())
                .ticketCount(ticketCount)
                .build();
    }


}
