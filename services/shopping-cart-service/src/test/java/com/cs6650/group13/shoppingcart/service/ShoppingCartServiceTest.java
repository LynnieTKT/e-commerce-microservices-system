package com.cs6650.group13.shoppingcart.service;

import com.cs6650.group13.shoppingcart.exception.CartNotFoundException;
import com.cs6650.group13.shoppingcart.messaging.OrderMessageProducer;
import com.cs6650.group13.shoppingcart.model.ShoppingCart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

  @Mock
  private CreditCardAuthorizerClient ccaClient;

  @Mock
  private OrderMessageProducer messageProducer;

  private ShoppingCartService shoppingCartService;

  @BeforeEach
  void setUp() {
    shoppingCartService = new ShoppingCartService(ccaClient);

    // Inject mocked dependencies
    ReflectionTestUtils.setField(shoppingCartService, "messageProducer", messageProducer);
    ReflectionTestUtils.setField(shoppingCartService, "rabbitmqEnabled", true);

    // Use lenient() to avoid UnnecessaryStubbingException
    lenient().when(messageProducer.sendOrderToWarehouse(anyInt(), any(ShoppingCart.class)))
        .thenReturn(true);
  }

  @Test
  void testCreateCart_Success() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);

    assertNotNull(cartId);
    assertTrue(cartId > 0);
  }

  @Test
  void testCreateCart_MultipleCartsHaveUniqueIds() {
    Integer customerId1 = 100;
    Integer customerId2 = 200;

    Integer cartId1 = shoppingCartService.createCart(customerId1);
    Integer cartId2 = shoppingCartService.createCart(customerId2);

    assertNotEquals(cartId1, cartId2);
  }

  @Test
  void testAddItem_Success() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    Integer productId = 5;
    Integer quantity = 2;

    shoppingCartService.addItem(cartId, productId, quantity);

    ShoppingCart cart = shoppingCartService.getCartById(cartId);
    assertEquals(1, cart.getItems().size());
    assertEquals(quantity, cart.getItems().get(productId).getQuantity());
  }

  @Test
  void testAddItem_SameProductTwice_QuantityAdds() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    Integer productId = 5;

    shoppingCartService.addItem(cartId, productId, 2);
    shoppingCartService.addItem(cartId, productId, 3);

    ShoppingCart cart = shoppingCartService.getCartById(cartId);
    assertEquals(5, cart.getItems().get(productId).getQuantity());
  }

  @Test
  void testAddItem_MultipleProducts() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);

    shoppingCartService.addItem(cartId, 5, 2);
    shoppingCartService.addItem(cartId, 10, 1);
    shoppingCartService.addItem(cartId, 15, 3);

    ShoppingCart cart = shoppingCartService.getCartById(cartId);
    assertEquals(3, cart.getItems().size());
  }

  @Test
  void testAddItem_CartNotFound_ThrowsException() {
    Integer invalidCartId = 999;
    Integer productId = 5;
    Integer quantity = 2;

    assertThrows(CartNotFoundException.class, () -> {
      shoppingCartService.addItem(invalidCartId, productId, quantity);
    });
  }

  @Test
  void testAddItem_AfterCheckout_ThrowsException() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    shoppingCartService.addItem(cartId, 5, 2);

    when(ccaClient.authorize(anyString())).thenReturn(true);
    shoppingCartService.checkout(cartId, "1234-5678-9012-3456");

    assertThrows(IllegalStateException.class, () -> {
      shoppingCartService.addItem(cartId, 10, 1);
    });
  }

  @Test
  void testCheckout_Success_ReturnsOrderId() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    shoppingCartService.addItem(cartId, 5, 2);
    String creditCard = "1234-5678-9012-3456";

    when(ccaClient.authorize(creditCard)).thenReturn(true);

    Integer orderId = shoppingCartService.checkout(cartId, creditCard);

    assertNotNull(orderId);
    assertTrue(orderId >= 1000);
  }

  @Test
  void testCheckout_MultipleCheckouts_UniqueOrderIds() {
    Integer cartId1 = shoppingCartService.createCart(100);
    Integer cartId2 = shoppingCartService.createCart(200);
    shoppingCartService.addItem(cartId1, 5, 2);
    shoppingCartService.addItem(cartId2, 10, 1);

    when(ccaClient.authorize(anyString())).thenReturn(true);

    Integer orderId1 = shoppingCartService.checkout(cartId1, "1234-5678-9012-3456");
    Integer orderId2 = shoppingCartService.checkout(cartId2, "1234-5678-9012-3456");

    assertNotEquals(orderId1, orderId2);
  }

  @Test
  void testCheckout_EmptyCart_ThrowsException() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    String creditCard = "1234-5678-9012-3456";

    assertThrows(IllegalStateException.class, () -> {
      shoppingCartService.checkout(cartId, creditCard);
    });
  }

  @Test
  void testCheckout_AlreadyCheckedOut_ThrowsException() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    shoppingCartService.addItem(cartId, 5, 2);
    String creditCard = "1234-5678-9012-3456";

    when(ccaClient.authorize(anyString())).thenReturn(true);
    shoppingCartService.checkout(cartId, creditCard);

    assertThrows(IllegalStateException.class, () -> {
      shoppingCartService.checkout(cartId, creditCard);
    });
  }

  @Test
  void testCheckout_CartNotFound_ThrowsException() {
    Integer invalidCartId = 999;
    String creditCard = "1234-5678-9012-3456";

    assertThrows(CartNotFoundException.class, () -> {
      shoppingCartService.checkout(invalidCartId, creditCard);
    });
  }

  @Test
  void testCheckout_PaymentDeclined_ThrowsException() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    shoppingCartService.addItem(cartId, 5, 2);
    String creditCard = "1234-5678-9012-3456";

    when(ccaClient.authorize(creditCard)).thenReturn(false);

    IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
      shoppingCartService.checkout(cartId, creditCard);
    });

    assertEquals("Payment declined", exception.getMessage());
  }

  @Test
  void testCheckout_InvalidCardFormat_ThrowsException() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    shoppingCartService.addItem(cartId, 5, 2);
    String invalidCard = "invalid-card";

    when(ccaClient.authorize(invalidCard))
        .thenThrow(new IllegalArgumentException("Invalid credit card format"));

    assertThrows(IllegalArgumentException.class, () -> {
      shoppingCartService.checkout(cartId, invalidCard);
    });
  }

  @Test
  void testCheckout_MarksCartAsCheckedOut() {
    Integer customerId = 100;
    Integer cartId = shoppingCartService.createCart(customerId);
    shoppingCartService.addItem(cartId, 5, 2);

    when(ccaClient.authorize(anyString())).thenReturn(true);

    shoppingCartService.checkout(cartId, "1234-5678-9012-3456");

    ShoppingCart cart = shoppingCartService.getCartById(cartId);
    assertTrue(cart.isCheckedOut());
  }
}