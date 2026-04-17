package com.app.ecom.service;

import com.app.ecom.dto.ProductResponse;
import com.app.ecom.model.Product;
import com.app.ecom.repository.OrderRepository;
import com.app.ecom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getRecommendations(Long userId, int limit) {
        List<String> categories = orderRepository.findDistinctCategoriesByUserId(userId);
        PageRequest pageable = PageRequest.of(0, limit);

        List<Product> products;
        if (categories != null && !categories.isEmpty()) {
            products = productRepository.findByCategoryIn(categories, pageable).getContent();
        } else {
            products = productRepository.findActiveProducts(PageRequest.of(0, limit * 3)).getContent();
        }

        if (products.isEmpty()) {
            products = productRepository.findActiveProducts(PageRequest.of(0, limit * 3)).getContent();
        }

        java.util.Collections.shuffle(products);
        return products.stream()
                .limit(limit)
                .map(this::mapToProductResponse)
                .toList();
    }

    private ProductResponse mapToProductResponse(Product savedProduct) {
        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(savedProduct.getId());
        productResponse.setName(savedProduct.getName());
        productResponse.setDescription(savedProduct.getDescription());
        productResponse.setImageUrl(savedProduct.getImageUrl());
        productResponse.setPrice(savedProduct.getPrice());
        productResponse.setCategory(savedProduct.getCategory());
        productResponse.setStockQuantity(savedProduct.getStockQuantity());
        productResponse.setActive(savedProduct.getActive());
        return productResponse;
    }
}
