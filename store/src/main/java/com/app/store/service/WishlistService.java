package com.app.store.service;

import com.app.store.model.entity.Wishlist;
import com.app.store.model.entity.WishlistItem;
import com.app.store.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final Scheduler jdbcScheduler;
    private final TransactionTemplate transactionTemplate;

    public Mono<Wishlist> getWishlist(String studentId) {
        return Mono.fromCallable(() -> wishlistRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setStudentId(studentId);
                    return wishlistRepository.save(newWishlist);
                })).subscribeOn(jdbcScheduler);
    }
    
    public Mono<Wishlist> toggleWishlistItem(String studentId, Long productId) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            Wishlist wishlist = wishlistRepository.findByStudentId(studentId).orElseGet(() -> {
                Wishlist newWishlist = new Wishlist();
                newWishlist.setStudentId(studentId);
                return wishlistRepository.save(newWishlist);
            });

            Optional<WishlistItem> existingItem = wishlist.getItems().stream()
                    .filter(item -> item.getProductId().equals(productId))
                    .findFirst();

            if (existingItem.isPresent()) {
                wishlist.getItems().remove(existingItem.get());
            } else {
                WishlistItem newItem = new WishlistItem();
                newItem.setProductId(productId);
                newItem.setAddedAt(LocalDateTime.now());
                wishlist.getItems().add(newItem);
            }
            return wishlistRepository.save(wishlist);
        })).subscribeOn(jdbcScheduler);
    }
}
