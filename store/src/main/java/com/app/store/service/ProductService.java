package com.app.store.service;

import com.app.store.dto.response.ProductResponse;
import com.app.store.dto.request.ProductRequest;
import com.app.store.exception.ProductNotFoundException;
import com.app.store.mapper.ProductMapper;
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
    private final ProductMapper productMapper;
    
    public Flux<ProductResponse> getAvailableProducts() {
        return Mono.fromCallable(() -> productRepository.findByStatusAndTotalStockGreaterThan("active", 0))
                .subscribeOn(jdbcScheduler)
                .flatMapIterable(list -> list)
                .map(productMapper::toResponse);
    }
    
    public Mono<ProductResponse> getProductById(Long id) {
        return Mono.fromCallable(() -> productRepository.findById(id))
                .subscribeOn(jdbcScheduler)
                .flatMap(Mono::justOrEmpty)
                .map(productMapper::toResponse)
                .switchIfEmpty(Mono.error(() -> new ProductNotFoundException("Product not found with id: " + id)));
    }

    public Mono<ProductResponse> createProduct(ProductRequest request) {
        return Mono.fromCallable(() -> {
            com.app.store.model.entity.Product product = productMapper.toEntity(request);
            product.setInstituteId("default-institute"); 
            com.app.store.model.entity.Product saved = productRepository.save(product);
            return productMapper.toResponse(saved);
        }).subscribeOn(jdbcScheduler);
    }

    public Mono<ProductResponse> updateProduct(Long id, ProductRequest request) {
        return Mono.fromCallable(() -> {
            com.app.store.model.entity.Product product = productRepository.findById(id)
                    .orElseThrow(() -> new ProductNotFoundException("Product not found"));
            
            productMapper.updateEntity(request, product);
            
            com.app.store.model.entity.Product saved = productRepository.save(product);
            return productMapper.toResponse(saved);
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
