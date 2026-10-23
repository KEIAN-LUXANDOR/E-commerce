package com.keianluxandor.backend.cart;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.keianluxandor.backend.catalog.ProductService;
import com.keianluxandor.backend.catalog.SellableVariant;
import com.keianluxandor.backend.common.ConflictException;
import com.keianluxandor.backend.common.NotFoundException;

@Service
public class CartService {

    private static final int MAX_ITEMS = 20;

    private final CartItemRepository cartRepository;
    private final ProductService productService;

    public CartService(CartItemRepository cartRepository, ProductService productService) {
        this.cartRepository = cartRepository;
        this.productService = productService;
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(UUID userId) {
        List<CartItem> items = cartRepository.findByUserIdOrderByCreatedAtDescIdAsc(userId);

        Map<UUID, SellableVariant> variants = productService.findSellableVariants(
                items.stream().map(CartItem::getVariantId).toList());

        // Items whose product was archived or removed are not shown
        List<CartLineResponse> lines = items.stream()
                .filter(item -> variants.containsKey(item.getVariantId()))
                .map(item -> {
                    SellableVariant variant = variants.get(item.getVariantId());
                    return new CartLineResponse(
                            item.getVariantId(),
                            variant.product(),
                            variant.pricePaise(),
                            variant.available(),
                            item.getCreatedAt());
                })
                .toList();

        // Live prices only; sold items are not counted
        long subtotalPaise = lines.stream()
                .filter(CartLineResponse::available)
                .mapToLong(CartLineResponse::pricePaise)
                .sum();

        boolean hasUnavailableItems = lines.stream().anyMatch(line -> !line.available());

        return new CartResponse(lines, lines.size(), subtotalPaise, hasUnavailableItems);
    }

    @Transactional
    public void addItem(UUID userId, UUID variantId) {
        if (cartRepository.existsByUserIdAndVariantId(userId, variantId)) {
            return; // already in the cart
        }

        SellableVariant variant = productService.findSellableVariants(List.of(variantId)).get(variantId);
        if (variant == null || variant.pricePaise() == null) {
            throw new NotFoundException("Product not found.");
        }
        if (!variant.available()) {
            throw new ConflictException("Sorry, this diamond has just been sold.");
        }
        if (cartRepository.countByUserId(userId) >= MAX_ITEMS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Your cart can hold up to " + MAX_ITEMS + " items. Please remove some first.");
        }

        cartRepository.addIfAbsent(userId, variantId);
    }

    @Transactional
    public void removeItem(UUID userId, UUID variantId) {
        cartRepository.removeItem(userId, variantId);
    }

    @Transactional
    public void clear(UUID userId) {
        cartRepository.clear(userId);
    }
}