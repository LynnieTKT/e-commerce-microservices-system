package com.cs6650.group13.shoppingcart.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShoppingCart {
  private Integer shoppingCartId;
  private Integer customerId;
  private Map<Integer, CartItem> items;
  private boolean checkedOut;

  public ShoppingCart() {
    this.items = new ConcurrentHashMap<>();
    this.checkedOut = false;
  }

  public ShoppingCart(Integer shoppingCartId, Integer customerId) {
    this.shoppingCartId = shoppingCartId;
    this.customerId = customerId;
    this.items = new ConcurrentHashMap<>();
    this.checkedOut = false;
  }

  public void addItem(Integer productId, Integer quantity) {
    if (items.containsKey(productId)) {
      CartItem existing = items.get(productId);
      existing.setQuantity(existing.getQuantity() + quantity);
    } else {
      items.put(productId, new CartItem(productId, quantity));
    }
  }

  public List<CartItem> getItemsList() {
    return new ArrayList<>(items.values());
  }

  public Integer getShoppingCartId() {
    return shoppingCartId;
  }

  public void setShoppingCartId(Integer shoppingCartId) {
    this.shoppingCartId = shoppingCartId;
  }

  public Integer getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Integer customerId) {
    this.customerId = customerId;
  }

  public Map<Integer, CartItem> getItems() {
    return items;
  }

  public void setItems(Map<Integer, CartItem> items) {
    this.items = items;
  }

  public boolean isCheckedOut() {
    return checkedOut;
  }

  public void setCheckedOut(boolean checkedOut) {
    this.checkedOut = checkedOut;
  }

  @Override
  public String toString() {
    return "ShoppingCart{" +
        "shoppingCartId=" + shoppingCartId +
        ", customerId=" + customerId +
        ", itemCount=" + items.size() +
        ", checkedOut=" + checkedOut +
        '}';
  }
}