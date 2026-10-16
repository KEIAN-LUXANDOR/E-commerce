package com.keianluxandor.backend.catalog;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keianluxandor.backend.common.NotFoundException;
import com.keianluxandor.backend.common.PageResponse;

@Service
public class ProductService {

    private static final int MAX_PAGE_SIZE = 50;

    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductVariantRepository productVariantRepository;

    public ProductService(ProductRepository productRepository,
                          ProductImageRepository productImageRepository,
                          ProductVariantRepository productVariantRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.productVariantRepository = productVariantRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> searchActiveProducts(String q, String categorySlug,
                                                              Long minPricePaise, Long maxPricePaise,
                                                              String sort, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Sort order = "newest".equals(sort)
                ? Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"))
                : Sort.by("name").and(Sort.by("id"));

        Specification<Product> filters = Specification.allOf(
                ProductSpecifications.isActive(),
                ProductSpecifications.nameContains(q),
                ProductSpecifications.inCategory(categorySlug),
                ProductSpecifications.priceBetween(minPricePaise, maxPricePaise));

        Page<Product> result = productRepository.findAll(filters, PageRequest.of(safePage, safeSize, order));

        return new PageResponse<>(
                toResponses(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProductsInCategory(UUID categoryId) {
        return toResponses(productRepository.findByCategoryIdAndStatusOrderByNameAsc(categoryId, ProductStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public ProductResponse getActiveProduct(String slug) {
        Product product = productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Product not found: " + slug));

        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrderAsc(product.getId());
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndActiveTrueOrderBySortOrderAsc(product.getId());

        return ProductResponse.from(product, images, variants);
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }

        List<UUID> productIds = products.stream().map(Product::getId).toList();

        Map<UUID, List<ProductImage>> imagesByProduct = productImageRepository
                .findByProductIdInOrderBySortOrderAsc(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));

        Map<UUID, List<ProductVariant>> variantsByProduct = productVariantRepository
                .findByProductIdInAndActiveTrueOrderBySortOrderAsc(productIds)
                .stream()
                .collect(Collectors.groupingBy(ProductVariant::getProductId));

        return products.stream()
                .map(product -> ProductResponse.from(
                        product,
                        imagesByProduct.getOrDefault(product.getId(), List.of()),
                        variantsByProduct.getOrDefault(product.getId(), List.of())))
                .toList();
    }
}