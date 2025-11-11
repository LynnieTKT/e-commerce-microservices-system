package com.ecommerce.productservicebad.controller;

import com.ecommerce.productservicebad.model.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
@RestController
public class ProductController {

    private final Random random = new Random();

    @PostMapping("/product")
    public ResponseEntity<Void> createProduct(@RequestBody Product product) {
        log.info("Received product: {}", product);
        
        // 50% 概率返回503错误
        if (random.nextBoolean()) {
            log.warn("Returning 503 Service Unavailable for product: {}", product.getProductId());
            return ResponseEntity.status(503).build();
        }
        
        // 50% 概率正常返回201
        log.info("Successfully processed product: {}", product.getProductId());
        return ResponseEntity.status(201).build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service Bad is running (50% error rate)");
    }
}

