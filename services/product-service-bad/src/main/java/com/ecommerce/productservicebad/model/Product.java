package com.ecommerce.productservicebad.model;

import lombok.Data;

@Data
public class Product {
    private Integer productId;
    private String sku;
    private String manufacturer;
    private Integer categoryId;
    private Integer weight;
    private Integer someOtherId;
}

