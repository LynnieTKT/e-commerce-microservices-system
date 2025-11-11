package com.ecommerce.productservice.controller;

import com.ecommerce.productservice.model.Product;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class ProductController {

    @PostMapping("/product")
    public ResponseEntity<Void> createProduct(@RequestBody Product product) {
        log.info("Received product: {}", product);
        // 记录日志，正常返回201
        return ResponseEntity.status(201).build();
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product Service is healthy");
    }
}

