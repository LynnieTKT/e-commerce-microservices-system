package com.cs6650.group13.producttester.model;

/**
 * Product model for testing
 */
public class Product {
    private Integer productId;
    private String sku;
    private String manufacturer;
    private Integer categoryId;
    private Integer weight;
    private Integer someOtherId;

    public Product(Integer productId, String sku, String manufacturer, Integer categoryId, Integer weight, Integer someOtherId) {
        this.productId = productId;
        this.sku = sku;
        this.manufacturer = manufacturer;
        this.categoryId = categoryId;
        this.weight = weight;
        this.someOtherId = someOtherId;
    }

    // Getters and Setters
    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    public Integer getSomeOtherId() {
        return someOtherId;
    }

    public void setSomeOtherId(Integer someOtherId) {
        this.someOtherId = someOtherId;
    }

    @Override
    public String toString() {
        return "Product{" +
                "productId=" + productId +
                ", sku='" + sku + '\'' +
                ", manufacturer='" + manufacturer + '\'' +
                ", categoryId=" + categoryId +
                ", weight=" + weight +
                ", someOtherId=" + someOtherId +
                '}';
    }
}

