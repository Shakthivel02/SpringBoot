package com.app.store.service;

import com.app.store.dto.response.ProductResponse;
import com.app.store.exception.ProductNotFoundException;
import com.app.store.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

@Service
@RequiredArgsConstructor
public class ProductService {
    
    private final ProductRepository productRepository;
    private final Scheduler jdbcScheduler;
    
    public Flux<ProductResponse> getAvailableProducts() {
        return Mono.fromCallable(() -> productRepository.findByStatusAndTotalStockGreaterThan("active", 0))
                .subscribeOn(jdbcScheduler)
                .flatMapIterable(list -> list)
                .map(ProductResponse::fromEntity);
    }
    
    public Mono<ProductResponse> getProductById(Long id) {
        return Mono.fromCallable(() -> productRepository.findById(id))
                .subscribeOn(jdbcScheduler)
                .flatMap(optionalProduct -> optionalProduct
                        .map(product -> Mono.just(ProductResponse.fromEntity(product)))
                        .orElseGet(() -> Mono.error(new ProductNotFoundException("Product not found with id: " + id))));
    }

    public Mono<ProductResponse> createProduct(com.app.store.dto.request.ProductRequest request) {
        return Mono.fromCallable(() -> {
            com.app.store.model.entity.Product product = new com.app.store.model.entity.Product();
            product.setProductTitle(request.getProductTitle());
            product.setDescription(request.getDescription());
            product.setBrandName(request.getBrandName());
            product.setImageUrl(request.getImageUrl());
            product.setTotalStock(request.getTotalStock());
            product.setStatus(request.getStatus());
            product.setPricing(request.getPricing());
            
            com.app.store.model.entity.Product saved = productRepository.save(product);
            return ProductResponse.fromEntity(saved);
        }).subscribeOn(jdbcScheduler);
    }

    public Mono<ProductResponse> updateProduct(Long id, com.app.store.dto.request.ProductRequest request) {
        return Mono.fromCallable(() -> {
            com.app.store.model.entity.Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));
                    
            product.setProductTitle(request.getProductTitle());
            product.setDescription(request.getDescription());
            product.setBrandName(request.getBrandName());
            product.setImageUrl(request.getImageUrl());
            product.setTotalStock(request.getTotalStock());
            product.setStatus(request.getStatus());
            product.setPricing(request.getPricing());
            
            com.app.store.model.entity.Product saved = productRepository.save(product);
            return ProductResponse.fromEntity(saved);
        }).subscribeOn(jdbcScheduler);
    }

    public Mono<Void> deleteProduct(Long id) {
        return Mono.fromRunnable(() -> {
            if (!productRepository.existsById(id)) {
                throw new ProductNotFoundException("Product not found");
            }
            productRepository.deleteById(id);
        }).subscribeOn(jdbcScheduler).then();
    }
}
