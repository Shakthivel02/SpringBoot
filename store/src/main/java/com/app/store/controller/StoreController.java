package com.app.store.controller;

import com.app.store.dto.request.PurchaseRequest;
import com.app.store.dto.response.ProductResponse;
import com.app.store.dto.response.StoreOrderResponse;
import com.app.store.service.CheckoutService;
import com.app.store.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final ProductService productService;
    private final CheckoutService checkoutService;

    @GetMapping("/products")
    public Flux<ProductResponse> getAvailableProducts() {
        return productService.getAvailableProducts();
    }

    @GetMapping("/products/{id}")
    public Mono<ProductResponse> getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    // Admin Routes
    @PostMapping("/admin/products")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ProductResponse> createProduct(@Valid @RequestBody com.app.store.dto.request.ProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/admin/products/{id}")
    public Mono<ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody com.app.store.dto.request.ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/admin/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteProduct(@PathVariable("id") Long productId) {
        return productService.deleteProduct(productId);
    }

    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<StoreOrderResponse> purchaseProduct(
            @Valid @RequestBody PurchaseRequest request,
            @RequestHeader("X-Student-Id") String studentId) {
        return checkoutService.requestStorePurchase(request, studentId);
    }

    @PostMapping("/checkout-cart")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<StoreOrderResponse> checkoutCart(@RequestHeader("X-Student-Id") String studentId) {
        return checkoutService.checkoutCart(studentId);
    }
}
