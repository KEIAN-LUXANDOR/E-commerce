package com.keianluxandor.backend.catalog;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keianluxandor.backend.common.NotFoundException;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductService productService;

    public CategoryService(CategoryRepository categoryRepository, ProductService productService) {
        this.categoryRepository = categoryRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listProductsInCategory(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Category not found: " + slug));

        return productService.listActiveProductsInCategory(category.getId());
    }
}