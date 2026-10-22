package com.keianluxandor.backend.wishlist;

import java.util.List;
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

// The customer is always taken from the login session, never from the request
@RestController
@RequestMapping("/api/v1/account/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public List<WishlistItemResponse> list(@AuthenticationPrincipal AuthUser user) {
        return wishlistService.list(user.id());
    }

    @GetMapping("/ids")
    public List<UUID> ids(@AuthenticationPrincipal AuthUser user) {
        return wishlistService.productIds(user.id());
    }

    @PutMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@AuthenticationPrincipal AuthUser user, @PathVariable UUID productId) {
        wishlistService.add(user.id(), productId);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@AuthenticationPrincipal AuthUser user, @PathVariable UUID productId) {
        wishlistService.remove(user.id(), productId);
    }
}