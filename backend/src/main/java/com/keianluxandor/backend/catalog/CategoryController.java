package com.keianluxandor.backend.catalog;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> listCategories() {
        return categoryService.listCategories();
    }

    @GetMapping("/{slug}/products")
    public List<ProductResponse> listProductsInCategory(@PathVariable("slug") String slug) {
        return categoryService.listProductsInCategory(slug);
    }
}