package com.app.store.service;

import com.app.store.dto.request.PurchaseRequest;
import com.app.store.dto.response.StoreOrderResponse;
import com.app.store.exception.InsufficientCoinsException;
import com.app.store.exception.OutOfStockException;
import com.app.store.exception.ProductNotFoundException;
import com.app.store.model.entity.Product;
import com.app.store.model.entity.StoreOrder;
import com.app.store.model.entity.StudentStats;
import com.app.store.model.entity.Transaction;
import com.app.store.repository.CartRepository;
import com.app.store.repository.ProductRepository;
import com.app.store.repository.StoreOrderRepository;
import com.app.store.repository.StudentStatsRepository;
import com.app.store.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ProductRepository productRepository;
    private final StudentStatsRepository studentStatsRepository;
    private final StoreOrderRepository storeOrderRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionTemplate transactionTemplate;
    private final CartRepository cartRepository;
    private final Scheduler jdbcScheduler;

    public Mono<StoreOrderResponse> requestStorePurchase(PurchaseRequest request, String studentId) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            
            // 1. Fetch Product
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));

            if (product.getTotalStock() < request.getQuantity()) {
                throw new OutOfStockException("Not enough stock available");
            }

            // 2. Fetch Student Stats
            StudentStats stats = studentStatsRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("Student stats not found"));

            // 3. Calculate Cost
            int cost = product.getPricing().getActivePrice().intValue() * request.getQuantity();

            if (stats.getTotalCoinsEarned() < cost) {
                throw new InsufficientCoinsException("Not enough coins for this purchase");
            }

            // 4. Deduct Coins
            int previousCoins = stats.getTotalCoinsEarned();
            stats.setTotalCoinsEarned(previousCoins - cost);
            studentStatsRepository.save(stats);

            // 5. Create Order
            StoreOrder order = new StoreOrder();
            order.setStudentId(studentId);
            order.setInstituteId(product.getInstituteId());
            order.setProductId(product.getId());
            order.setStatus("pending_parent"); // Initial status
            order.setCostCoins(cost);
            order.setQuantity(request.getQuantity());
            StoreOrder savedOrder = storeOrderRepository.save(order);

            // 6. Log Transaction
            Transaction transaction = new Transaction();
            transaction.setStudentId(studentId);
            transaction.setStoreOrderId(savedOrder.getId());
            transaction.setTransactionType("store_purchase");
            transaction.setStatus("pending");
            transaction.setPreviousCoins(previousCoins);
            transaction.setCoinsChange(-cost);
            Transaction savedTransaction = transactionRepository.save(transaction);

            // 7. Return robust response
            return StoreOrderResponse.builder()
                    .orderId(savedOrder.getId())
                    .status(savedOrder.getStatus())
                    .transactionId(savedTransaction.getId())
                    .coinsDeducted(cost)
                    .build();

        })).subscribeOn(jdbcScheduler);
    }

    public Mono<StoreOrderResponse> checkoutCart(String studentId) {
        return Mono.fromCallable(() -> transactionTemplate.execute(status -> {
            
            com.app.store.model.entity.Cart cart = cartRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("Cart is not found"));

            if (cart.getItems().isEmpty()) {
                throw new RuntimeException("Cart is empty");
            }

            StudentStats stats = studentStatsRepository.findByStudentId(studentId)
                    .orElseThrow(() -> new RuntimeException("Student stats not found"));

            int totalCost = 0;
            List<StoreOrder> orders = new ArrayList<>();

            for (com.app.store.model.entity.CartItem item : cart.getItems()) {
                Product product = productRepository.findById(item.getProductId())
                        .orElseThrow(() -> new ProductNotFoundException("Product " + item.getProductId() + " not found"));
                
                if (product.getTotalStock() < item.getQuantity()) {
                    throw new OutOfStockException("Not enough stock for " + product.getProductTitle());
                }

                int cost = product.getPricing().getActivePrice().intValue() * item.getQuantity();
                totalCost += cost;

                StoreOrder order = new StoreOrder();
                order.setStudentId(studentId);
                order.setInstituteId(product.getInstituteId());
                order.setProductId(product.getId());
                order.setStatus("pending_parent");
                order.setCostCoins(cost);
                order.setQuantity(item.getQuantity());
                orders.add(order);
            }

            if (stats.getTotalCoinsEarned() < totalCost) {
                throw new InsufficientCoinsException("Not enough coins. Need " + totalCost);
            }

            // Deduct Coins
            int previousCoins = stats.getTotalCoinsEarned();
            stats.setTotalCoinsEarned(previousCoins - totalCost);
            studentStatsRepository.save(stats);

            storeOrderRepository.saveAll(orders);

            Transaction transaction = new Transaction();
            transaction.setStudentId(studentId);
            transaction.setTransactionType("cart_purchase");
            transaction.setStatus("pending");
            transaction.setPreviousCoins(previousCoins);
            transaction.setCoinsChange(-totalCost);
            Transaction savedTransaction = transactionRepository.save(transaction);

            cart.getItems().clear();
            cartRepository.save(cart);

            return StoreOrderResponse.builder()
                    .orderId(null) // multiple orders generated
                    .status("pending_parent")
                    .transactionId(savedTransaction.getId())
                    .coinsDeducted(totalCost)
                    .build();

        })).subscribeOn(jdbcScheduler);
    }
}
