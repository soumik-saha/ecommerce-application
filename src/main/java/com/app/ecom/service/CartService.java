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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    public Boolean addToCart(String userId, CartItemRequest cartItemRequest) {
        Optional<Product> productOptional = productRepository.findById(cartItemRequest.getProductId());
        if(productOptional.isEmpty()) {
            return false;
        }

        Product product = productOptional.get();
        if(product.getStockQuantity()<cartItemRequest.getQuantity()) {
            return false;
        }

        Optional<User> userOptional = userRepository.findById(Long.valueOf(userId));
        if(userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();

        CartItem existingCartItem = cartItemRepository.findByUserAndProduct(user, product);
        if(existingCartItem != null) {
            // Update the quantity and price of existing Cart Item
            existingCartItem.setQuantity(existingCartItem.getQuantity()+cartItemRequest.getQuantity());
            existingCartItem.setPrice(product.getPrice().multiply(BigDecimal.valueOf(existingCartItem.getQuantity())));
            cartItemRepository.save(existingCartItem);
        } else {
            // Create new cart item
            CartItem cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(cartItemRequest.getQuantity());
            cartItem.setPrice(product.getPrice().multiply(BigDecimal.valueOf(cartItemRequest.getQuantity())));
            cartItemRepository.save(cartItem);
        }

        return true;
    }

    @Transactional
    public Boolean deleteProductFromCart(String userId, Long productId) {
        Optional<Product> productOptional = productRepository.findById(productId);
        if(productOptional.isEmpty()) {
            return false;
        }

        Optional<User> userOptional = userRepository.findById(Long.valueOf(userId));
        if(userOptional.isEmpty()) {
            return false;
        }

        User user = userOptional.get();
        Product product = productOptional.get();
        CartItem cartItem = cartItemRepository.findByUserAndProduct(user, product);
        cartItemRepository.delete(cartItem);
        return true;
    }

    public List<CartItemResponse> fetchItemsFromCart(String userId) {
        List<CartItem> cartItems = cartItemRepository.getCartItemsByUserId(Long.valueOf(userId));
        List<CartItemResponse> cartItemResponses = new ArrayList<>();

        for(CartItem cartItem : cartItems) {
            CartItemResponse cartItemResponse = new CartItemResponse();
            ProductResponse product = productService.getProductById(cartItem.getProduct().getId());
            cartItemResponse.setProduct(product);
            cartItemResponse.setQuantity(cartItem.getQuantity());
            cartItemResponse.setPrice(cartItem.getPrice());
            cartItemResponses.add(cartItemResponse);
        }

        return cartItemResponses;
    }
}
