package com.keianluxandor.backend.catalog;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.keianluxandor.backend.common.NotFoundException;

@Service
public class ProductService {

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
    public List<ProductResponse> listActiveProducts() {
        return toResponses(productRepository.findByStatusOrderByNameAsc(ProductStatus.ACTIVE));
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