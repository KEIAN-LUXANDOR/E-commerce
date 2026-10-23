package com.keianluxandor.backend.cart;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.keianluxandor.backend.auth.AuthUser;

@RestController
@RequestMapping("/api/v1/account/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal AuthUser user) {
        return cartService.getCart(user.id());
    }

    @PutMapping("/items/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addItem(@AuthenticationPrincipal AuthUser user, @PathVariable("variantId") UUID variantId) {
        cartService.addItem(user.id(), variantId);
    }

    @DeleteMapping("/items/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(@AuthenticationPrincipal AuthUser user, @PathVariable("variantId") UUID variantId) {
        cartService.removeItem(user.id(), variantId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal AuthUser user) {
        cartService.clear(user.id());
    }
}