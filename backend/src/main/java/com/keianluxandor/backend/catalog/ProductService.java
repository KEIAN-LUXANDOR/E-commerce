package com.keianluxandor.backend.catalog;

import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                                                              DiamondFilter diamondFilter,
                                                              String sort, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Specification<Product> filters = Specification.allOf(
                ProductSpecifications.isActive(),
                ProductSpecifications.nameContains(q),
                ProductSpecifications.inCategory(categorySlug),
                ProductSpecifications.priceBetween(minPricePaise, maxPricePaise),
                DiamondSpecifications.matches(diamondFilter));

        Page<Product> result = productRepository.findAll(filters,
                PageRequest.of(safePage, safeSize, sortOrder(sort)));

        return new PageResponse<>(
                toResponses(result.getContent()),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    // Sort options from the shop page. Unknown values fall back to "recommended".
    private static Sort sortOrder(String sort) {
        Sort order = switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "fromPricePaise");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "fromPricePaise");
            case "carat_asc" -> Sort.by(Sort.Direction.ASC, "carat");
            case "carat_desc" -> Sort.by(Sort.Direction.DESC, "carat");
            case "name" -> Sort.by("name");
            default -> Sort.by(Sort.Direction.DESC, "createdAt"); // "recommended" and "newest"
        };
        return order.and(Sort.by("id"));
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

    // Used by the wishlist: true if the product exists and is ACTIVE
    @Transactional(readOnly = true)
    public boolean isActiveProduct(UUID productId) {
        return productRepository.findById(productId)
                .map(product -> product.getStatus() == ProductStatus.ACTIVE)
                .orElse(false);
    }

    // Used by the wishlist: ACTIVE products for these ids, in the same order as the ids
    @Transactional(readOnly = true)
    public List<ProductResponse> getActiveProductsByIds(List<UUID> productIds) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, Product> activeById = productRepository.findAllById(productIds).stream()
                .filter(product -> product.getStatus() == ProductStatus.ACTIVE)
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<Product> ordered = productIds.stream()
                .map(activeById::get)
                .filter(Objects::nonNull)
                .toList();

        return toResponses(ordered);
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