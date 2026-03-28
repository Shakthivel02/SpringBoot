package com.app.store.controller;

import com.app.store.model.entity.Cart;
import com.app.store.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/store/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public Mono<Cart> getCart(@RequestHeader("X-Student-Id") String studentId) {
        return cartService.getCart(studentId);
    }

    @PostMapping("/items/{productId}")
    public Mono<Cart> updateCartItem(
            @RequestHeader("X-Student-Id") String studentId,
            @PathVariable Long productId,
            @RequestParam Integer quantity) {
        return cartService.updateCartItem(studentId, productId, quantity);
    }
}
