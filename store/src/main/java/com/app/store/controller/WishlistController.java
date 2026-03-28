package com.app.store.controller;

import com.app.store.model.entity.Wishlist;
import com.app.store.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/store/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    @GetMapping
    public Mono<Wishlist> getWishlist(@RequestHeader("X-Student-Id") String studentId) {
        return wishlistService.getWishlist(studentId);
    }

    @PostMapping("/toggle/{productId}")
    public Mono<Wishlist> toggleWishlistItem(
            @RequestHeader("X-Student-Id") String studentId,
            @PathVariable Long productId) {
        return wishlistService.toggleWishlistItem(studentId, productId);
    }
}
