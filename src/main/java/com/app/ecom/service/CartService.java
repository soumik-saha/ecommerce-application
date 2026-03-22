package com.app.ecom.service;

import com.app.ecom.dto.CartItemRequest;
import com.app.ecom.dto.CartItemResponse;
import com.app.ecom.dto.ProductResponse;
import com.app.ecom.model.CartItem;
import com.app.ecom.model.Product;
import com.app.ecom.model.User;
import com.app.ecom.repository.CartItemRepository;
import com.app.ecom.repository.ProductRepository;
import com.app.ecom.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public Boolean addToCart(String userId, CartItemRequest cartItemRequest) {
        log.info("Adding to cart for userId={}, productId={}, quantity={}", userId, cartItemRequest.getProductId(), cartItemRequest.getQuantity());
        Optional<Product> productOptional = productRepository.findById(cartItemRequest.getProductId());
        if(productOptional.isEmpty()) {
            log.warn("Add to cart failed: productId={} not found", cartItemRequest.getProductId());
            return false;
        }

        Product product = productOptional.get();
        if(product.getStockQuantity()<cartItemRequest.getQuantity()) {
            log.warn("Add to cart failed: insufficient stock for productId={}, requested={}, available={}",
                    cartItemRequest.getProductId(), cartItemRequest.getQuantity(), product.getStockQuantity());
            return false;
        }

        Optional<User> userOptional = userRepository.findById(Long.valueOf(userId));
        if(userOptional.isEmpty()) {
            log.warn("Add to cart failed: userId={} not found", userId);
            return false;
        }

        User user = userOptional.get();

        CartItem existingCartItem = cartItemRepository.findByUserAndProduct(user, product);
        if(existingCartItem != null) {
            // Update the quantity and price of existing Cart Item
            existingCartItem.setQuantity(existingCartItem.getQuantity()+cartItemRequest.getQuantity());
            existingCartItem.setPrice(product.getPrice().multiply(BigDecimal.valueOf(existingCartItem.getQuantity())));
            cartItemRepository.save(existingCartItem);
            log.info("Updated existing cart item for userId={}, productId={}, quantity={}",
                    userId, cartItemRequest.getProductId(), existingCartItem.getQuantity());
        } else {
            // Create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(cartItemRequest.getQuantity());
            cartItem.setPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItemRequest.getQuantity())));
            cartItemRepository.save(cartItem);
            log.info("Created new cart item for userId={}, productId={}, quantity={}",
                    userId, cartItemRequest.getProductId(), cartItemRequest.getQuantity());
        }

        return true;
    }

    @Transactional
    public Boolean deleteProductFromCart(String userId, Long productId) {
        log.info("Removing cart item for userId={}, productId={}", userId, productId);
        Optional<Product> productOptional = productRepository.findById(productId);
        if(productOptional.isEmpty()) {
            log.warn("Delete from cart failed: productId={} not found", productId);
            return false;
        }

        Optional<User> userOptional = userRepository.findById(Long.valueOf(userId));
        if(userOptional.isEmpty()) {
            log.warn("Delete from cart failed: userId={} not found", userId);
            return false;
        }

        User user = userOptional.get();
        Product product = productOptional.get();
        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product);
        if (cartItem == null) {
            log.warn("Delete from cart failed: cart item not found for userId={}, productId={}", userId, productId);
            return false;
        }
        cartItemRepository.delete(cartItem);
        log.info("Removed cart item for userId={}, productId={}", userId, productId);
        return true;
    }

    public List<CartItemResponse> fetchItemsFromCart(String userId) {
        log.info("Fetching cart items for userId={}", userId);
        List<CartItem> cartItems = cartItemRepository.getCartItemsByUserId(Long.valueOf(userId));
        List<CartItemResponse> cartItemResponses = new ArrayList<>();

        for(CartItem cartItem : cartItems) {
            CartItemResponse cartItemResponse = new CartItemResponse();
            Product product = productService.getProductById(cartItem.getProduct().getId());
            cartItemResponse.setProduct(product);
            cartItemResponse.setQuantity(cartItem.getQuantity());
            cartItemResponse.setPrice(cartItem.getPrice());
            cartItemResponses.add(cartItemResponse);
        }

        log.info("Fetched {} cart items for userId={}", cartItemResponses.size(), userId);
        return cartItemResponses;
    }
}
