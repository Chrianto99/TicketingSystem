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

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubcategoryService {
    // subcategories named this always sort last, in every list they appear in
    private static final String OTHER_NAME = "ΑΛΛΟ";

    private final SubcategoryRepository subcategoryRepository;
    private final CategoryRepository categoryRepository;
    private final TicketRepository ticketRepository;

    public SubcategoryResponse createSubcategory(SubcategoryRequest req){
        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new EntityNotFoundException("Η κατηγορία βλάβης δεν βρέθηκε"));

        Subcategory subcategory = new Subcategory();
        subcategory.setName(req.getName());
        subcategory.setCategory(category);
        subcategory.setActive(true);

        subcategory = subcategoryRepository.save(subcategory);

        return toResponse(subcategory);
    }

    @Transactional
    public SubcategoryResponse updateSubcategoryName(Long subcategoryId, String name) {
        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε υποκατηγορία με id: " + subcategoryId));

        subcategory.setName(name);
        subcategory = subcategoryRepository.save(subcategory);

        return toResponse(subcategory);
    }

    @Transactional
    public void toggleSubcategoryActiveState(Long subcategoryId) {

        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε υποκατηγορία με id: " + subcategoryId));

        boolean newState = !subcategory.isActive();
        if (newState && !subcategory.getCategory().isActive()) {
            throw new IllegalStateException("Δεν είναι δυνατή η ενεργοποίηση υποκατηγορίας ενώ η κατηγορία βλάβης της είναι ανενεργή");
        }
        subcategory.setActive(newState);
        subcategoryRepository.save(subcategory);
    }

    @Transactional
    public void deleteSubcategory(Long subcategoryId) {
        Subcategory subcategory = subcategoryRepository.findById(subcategoryId)
                .orElseThrow(() -> new EntityNotFoundException("Δεν βρέθηκε υποκατηγορία με id: " + subcategoryId));

        if (subcategory.isActive()) {
            throw new IllegalStateException("Μόνο απενεργοποιημένες υποκατηγορίες μπορούν να διαγραφούν");
        }

        ticketRepository.nullifySubcategory(subcategoryId);
        subcategoryRepository.deleteById(subcategoryId);
    }

    public List<SubcategoryResponse> getAllSubcategories(Long categoryId){
        List<Subcategory> subcategories = categoryId != null
                ? subcategoryRepository.findByCategoryId(categoryId)
                : subcategoryRepository.findAll();

        Map<Long, Long> ticketCounts = ticketRepository.countTicketsGroupedBySubcategory().stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));

        Collator collator = Collator.getInstance(new Locale("el", "GR"));
        return subcategories.stream()
                .map(s -> toResponse(s, ticketCounts.getOrDefault(s.getId(), 0L)))
                .sorted(Comparator.comparingInt((SubcategoryResponse s) -> OTHER_NAME.equalsIgnoreCase(s.getName()) ? 1 : 0)
                        .thenComparing(SubcategoryResponse::getName, (a, b) -> collator.compare(a, b)))
                .toList();
    }

    private SubcategoryResponse toResponse(Subcategory subcategory) {
        return toResponse(subcategory, ticketRepository.countBySubcategoryId(subcategory.getId()));
    }

    private SubcategoryResponse toResponse(Subcategory subcategory, long ticketCount) {
        return SubcategoryResponse.builder()
                .id(subcategory.getId())
                .name(subcategory.getName())
                .categoryId(subcategory.getCategory().getId())
                .categoryName(subcategory.getCategory().getName())
                .active(subcategory.isActive())
                .ticketCount(ticketCount)
                .build();
    }


}
