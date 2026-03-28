package com.app.store.service;

import com.app.store.model.entity.Cart;
import com.app.store.model.entity.CartItem;
import com.app.store.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {
    
    private final CartRepository cartRepository;
    private final Scheduler jdbcScheduler;
    private final TransactionTemplate transactionTemplate;
    private static final int CART_LIMIT = 50;
    
    public Mono<Cart> getCart(String studentId) {
        return Mono.fromCallable(() -> cartRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setStudentId(studentId);
                    return cartRepository.save(newCart);
                })).subscribeOn(jdbcScheduler);
    }
    
    public Mono<Cart> updateCartItem(String studentId, Long productId, Integer quantity) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Cart cart = cartRepository.findByStudentId(studentId).orElseGet(() -> {
                Cart newCart = new Cart();
                newCart.setStudentId(studentId);
                return cartRepository.save(newCart);
            });

            Optional<CartItem> existingItem = cart.getItems().stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst();

            if (quantity <= 0) {
                existingItem.ifPresent(item -> cart.getItems().remove(item));
            } else {
                if (existingItem.isPresent()) {
                    existingItem.get().setQuantity(quantity);
                } else {
                    if (cart.getItems().size() >= CART_LIMIT) {
                        throw new RuntimeException("Cart limit of " + CART_LIMIT + " distinct items reached.");
                    }
                    CartItem newItem = new CartItem();
                    newItem.setProductId(productId);
                    newItem.setQuantity(quantity);
                    newItem.setAddedAt(LocalDateTime.now());
                    cart.getItems().add(newItem);
                }
            }
            return cartRepository.save(cart);
        })).subscribeOn(jdbcScheduler);
    }
}
