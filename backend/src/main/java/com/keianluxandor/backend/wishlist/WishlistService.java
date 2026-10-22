package com.keianluxandor.backend.wishlist;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.catalog.ProductService;
import com.keianluxandor.backend.common.NotFoundException;

@Service
public class WishlistService {

    private static final int MAX_ITEMS = 100;

    private final WishlistItemRepository wishlistRepository;
    private final ProductService productService;

    public WishlistService(WishlistItemRepository wishlistRepository, ProductService productService) {
        this.wishlistRepository = wishlistRepository;
        this.productService = productService;
    }

    // Full product details, newest first. Products that are no longer ACTIVE are left out.
    @Transactional(readOnly = true)
    public List<WishlistItemResponse> list(UUID userId) {
        List<WishlistItem> items = wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<UUID, Instant> addedAt = items.stream()
                .collect(Collectors.toMap(WishlistItem::getProductId, WishlistItem::getCreatedAt));

        List<UUID> productIds = items.stream().map(WishlistItem::getProductId).toList();

        return productService.getActiveProductsByIds(productIds).stream()
                .map(product -> new WishlistItemResponse(product, addedAt.get(product.id())))
                .toList();
    }

    // Just the product ids, for showing filled hearts on the shop page
    @Transactional(readOnly = true)
    public List<UUID> productIds(UUID userId) {
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(WishlistItem::getProductId)
                .toList();
    }

    // Adding the same product twice does nothing
    @Transactional
    public void add(UUID userId, UUID productId) {
        if (!productService.isActiveProduct(productId)) {
            throw new NotFoundException("Product not found.");
        }
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            return;
        }
        if (wishlistRepository.countByUserId(userId) >= MAX_ITEMS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your wishlist can hold up to " + MAX_ITEMS + " products. Please remove some first.");
        }
        try {
            wishlistRepository.saveAndFlush(new WishlistItem(userId, productId));
        } catch (DataIntegrityViolationException e) {
            // Added at the same moment from another tab: it's already there, so nothing to do
        }
    }

    // Removing something that isn't there also succeeds
    @Transactional
    public void remove(UUID userId, UUID productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
    }
}