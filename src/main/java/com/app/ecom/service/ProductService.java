package com.app.ecom.service;

import com.app.ecom.dto.ProductRequest;
import com.app.ecom.dto.ProductResponse;
import com.app.ecom.exception.ResourceNotFoundException;
import com.app.ecom.model.Product;
import com.app.ecom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @CacheEvict(value = {"products", "productSearch"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest productRequest) {
        log.info("Creating product with name={}", productRequest.getName());
        Product product = new Product();
        updateProductFromRequest(product, productRequest);
        Product savedProduct = productRepository.save(product);
        log.info("Product created with id={}", savedProduct.getId());
        return mapToProductResponse(savedProduct);
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

    private void updateProductFromRequest(Product product, ProductRequest productRequest) {
        product.setName(productRequest.getName());
        product.setPrice(productRequest.getPrice());
        product.setCategory(productRequest.getCategory());
        product.setDescription(productRequest.getDescription());
        product.setImageUrl(productRequest.getImageUrl());
        product.setStockQuantity(productRequest.getStockQuantity());
    }

    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all active products");
        List<Product> products = productRepository.findByActiveTrue();
        List<ProductResponse> productResponseList = new ArrayList<>();
        for(Product product : products) {
            ProductResponse productResponse = mapToProductResponse(product);
            productResponseList.add(productResponse);
        }
        return productResponseList;
    }

    @Cacheable(value = "products", key = "#id")
    public Product getProductById(Long id) {
        log.info("Fetching product by id={}", id);
        return productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    public ProductResponse getProductResponseById(Long id) {
        return mapToProductResponse(getProductById(id));
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public void updateProduct(Long id, ProductRequest productRequest) {
        log.info("Updating product by id={}", id);
        Product existingProduct = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        updateProductFromRequest(existingProduct, productRequest);
        productRepository.save(existingProduct);
        log.info("Product updated in database for id={}", id);
    }

    @Caching(evict = {
            @CacheEvict(value = "products", key = "#id"),
            @CacheEvict(value = "productSearch", allEntries = true)
    })
    public void deleteProduct(Long id) {
        log.info("Soft deleting product by id={}", id);
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
        log.info("Product soft deleted for id={}", id);
    }

    @Cacheable(value = "productSearch", key = "#keyword")
    public List<ProductResponse> searchProducts(String keyword) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        log.info("Searching products with keyword='{}'", safeKeyword);
        List<Product> productList = productRepository.searchProducts(safeKeyword);
        List<ProductResponse> productResponseList = new ArrayList<>();
        for(Product product : productList) {
            ProductResponse productResponse = mapToProductResponse(product);
            productResponseList.add(productResponse);
        }
        return productResponseList;
    }

    public Page<ProductResponse> searchProductsPaged(String keyword, int page, int limit) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        log.info("Searching products with pagination keyword='{}', page={}, limit={}", safeKeyword, page, limit);
        PageRequest pageable = PageRequest.of(page, limit, Sort.by("createdAt").descending());
        return productRepository.searchActiveProducts(safeKeyword, pageable)
                .map(this::mapToProductResponse);
    }

    public Page<ProductResponse> getProducts(String keyword, int page, int size) {
        String safeKeyword = keyword == null ? "" : keyword.trim();
        log.info("Fetching paged products with keyword='{}', page={}, size={}", safeKeyword, page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productRepository.searchActiveProducts(safeKeyword, pageable)
                .map(this::mapToProductResponse);
    }
}
