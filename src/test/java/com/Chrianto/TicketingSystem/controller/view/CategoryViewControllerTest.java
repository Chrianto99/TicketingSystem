package com.Chrianto.TicketingSystem.controller.view;

import com.Chrianto.TicketingSystem.dto.request.CategoryRequest;
import com.Chrianto.TicketingSystem.dto.request.SubcategoryRequest;
import com.Chrianto.TicketingSystem.dto.response.CategoryResponse;
import com.Chrianto.TicketingSystem.dto.response.SubcategoryResponse;
import com.Chrianto.TicketingSystem.entity.User;
import com.Chrianto.TicketingSystem.entity.enums.UserRole;
import com.Chrianto.TicketingSystem.security.SecurityConfig;
import com.Chrianto.TicketingSystem.service.CategoryService;
import com.Chrianto.TicketingSystem.service.SubcategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(CategoryViewController.class)
@Import(SecurityConfig.class)
class CategoryViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;
    @MockitoBean
    private SubcategoryService subcategoryService;
    // Required to satisfy SecurityConfig's constructor; not exercised since
    // .with(user(...)) sets the SecurityContext directly instead of authenticating.
    @MockitoBean
    private UserDetailsService userDetailsService;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@ticketing.com");
        adminUser.setRole(UserRole.ADMIN);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setUsername("Chrianto");
        regularUser.setEmail("Chrianto@ece.auth.gr");
        regularUser.setRole(UserRole.USER);

        when(categoryService.getAllCategories()).thenReturn(List.of());
        when(subcategoryService.getAllSubcategories(null)).thenReturn(List.of());
    }

    @Test
    void listCategories_splitsCategoriesAndSubcategoriesIntoModelAttributes() throws Exception {
        CategoryResponse active = CategoryResponse.builder().id(1L).name("Hardware").active(true).build();
        CategoryResponse inactive = CategoryResponse.builder().id(2L).name("Legacy").active(false).build();
        when(categoryService.getAllCategories()).thenReturn(List.of(active, inactive));

        SubcategoryResponse activeSub = SubcategoryResponse.builder().id(10L).name("Printers").categoryId(1L).active(true).build();
        SubcategoryResponse inactiveSub = SubcategoryResponse.builder().id(11L).name("Old Printers").categoryId(1L).active(false).build();
        when(subcategoryService.getAllSubcategories(null)).thenReturn(List.of(activeSub, inactiveSub));

        mockMvc.perform(get("/categories")
                        .with(user(adminUser)))
                .andExpect(status().isOk())
                .andExpect(view().name("categories/list"))
                .andExpect(model().attribute("activeCategories", List.of(active)))
                .andExpect(model().attribute("inactiveCategories", List.of(inactive)))
                .andExpect(model().attribute("activeSubcategoriesByCategory", java.util.Map.of(1L, List.of(activeSub))))
                .andExpect(model().attribute("inactiveSubcategoriesByCategory", java.util.Map.of(1L, List.of(inactiveSub))))
                .andExpect(model().attributeExists("categoryRequest"));
    }

    @Test
    void listCategories_withoutAuthentication_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/categories"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(categoryService);
        verifyNoInteractions(subcategoryService);
    }

    @Test
    void createCategory_asAdminWithValidData_createsCategoryAndRedirects() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", "Hardware"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).createCategory(any(CategoryRequest.class));
    }

    @Test
    void createCategory_withBlankName_redirectsWithoutCreating() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService, never()).createCategory(any());
    }

    @Test
    void createCategory_asNonAdmin_isForbiddenAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(user(regularUser))
                        .with(csrf())
                        .param("name", "Hardware"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategory_withoutAuthentication_redirectsToLoginAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(csrf())
                        .param("name", "Hardware"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(categoryService);
    }

    @Test
    void createCategory_withoutCsrfToken_isForbiddenAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/categories")
                        .with(user(adminUser))
                        .param("name", "Hardware"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void editCategory_asAdminWithValidData_updatesCategoryAndRedirects() throws Exception {
        mockMvc.perform(post("/categories/5/edit")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", "Renamed Category"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).updateCategoryName(5L, "Renamed Category");
    }

    @Test
    void editCategory_asNonAdmin_isForbiddenAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/categories/5/edit")
                        .with(user(regularUser))
                        .with(csrf())
                        .param("name", "Renamed Category"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void editCategory_withoutCsrfToken_isForbiddenAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/categories/5/edit")
                        .with(user(adminUser))
                        .param("name", "Renamed Category"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void deleteCategory_asAdmin_deletesAndRedirects() throws Exception {
        mockMvc.perform(post("/categories/5/delete")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).deleteCategory(5L);
    }

    @Test
    void deleteCategory_asNonAdmin_isForbiddenAndDoesNotDelete() throws Exception {
        mockMvc.perform(post("/categories/5/delete")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void deleteCategory_withoutCsrfToken_isForbiddenAndDoesNotDelete() throws Exception {
        mockMvc.perform(post("/categories/5/delete")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void toggleCategoryActive_asAdmin_togglesAndRedirects() throws Exception {
        mockMvc.perform(post("/categories/5/toggle-active")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(categoryService).toggleCategoryActiveState(5L);
    }

    @Test
    void toggleCategoryActive_asNonAdmin_isForbiddenAndDoesNotToggle() throws Exception {
        mockMvc.perform(post("/categories/5/toggle-active")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void toggleCategoryActive_withoutCsrfToken_isForbiddenAndDoesNotToggle() throws Exception {
        mockMvc.perform(post("/categories/5/toggle-active")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    void createSubcategory_asAdminWithValidData_createsSubcategoryUnderParentCategoryAndRedirects() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", "Printers"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(subcategoryService).createSubcategory(argThat(req ->
                req.getName().equals("Printers") && req.getCategoryId().equals(5L)));
    }

    @Test
    void createSubcategory_asNonAdmin_isForbiddenAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories")
                        .with(user(regularUser))
                        .with(csrf())
                        .param("name", "Printers"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    void createSubcategory_withoutCsrfToken_isForbiddenAndDoesNotCreate() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories")
                        .with(user(adminUser))
                        .param("name", "Printers"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    void deleteSubcategory_asAdmin_deletesAndRedirects() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/delete")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(subcategoryService).deleteSubcategory(10L);
    }

    @Test
    void deleteSubcategory_asNonAdmin_isForbiddenAndDoesNotDelete() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/delete")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    void deleteSubcategory_withoutCsrfToken_isForbiddenAndDoesNotDelete() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/delete")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    void editSubcategory_asAdminWithValidData_updatesSubcategoryAndRedirects() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/edit")
                        .with(user(adminUser))
                        .with(csrf())
                        .param("name", "Renamed Subcategory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(subcategoryService).updateSubcategoryName(10L, "Renamed Subcategory");
    }

    @Test
    void editSubcategory_asNonAdmin_isForbiddenAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/edit")
                        .with(user(regularUser))
                        .with(csrf())
                        .param("name", "Renamed Subcategory"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    void editSubcategory_withoutCsrfToken_isForbiddenAndDoesNotUpdate() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/edit")
                        .with(user(adminUser))
                        .param("name", "Renamed Subcategory"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    void toggleSubcategoryActive_asAdmin_togglesAndRedirects() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/toggle-active")
                        .with(user(adminUser))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));

        verify(subcategoryService).toggleSubcategoryActiveState(10L);
    }

    @Test
    void toggleSubcategoryActive_asNonAdmin_isForbiddenAndDoesNotToggle() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/toggle-active")
                        .with(user(regularUser))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }

    @Test
    void toggleSubcategoryActive_withoutCsrfToken_isForbiddenAndDoesNotToggle() throws Exception {
        mockMvc.perform(post("/categories/5/subcategories/10/toggle-active")
                        .with(user(adminUser)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(subcategoryService);
    }
}
