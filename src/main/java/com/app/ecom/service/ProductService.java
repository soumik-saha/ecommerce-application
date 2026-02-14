package com.app.ecom.service;

import com.app.ecom.dto.ProductRequest;
import com.app.ecom.dto.ProductResponse;
import com.app.ecom.model.Product;
import com.app.ecom.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public ProductResponse createProduct(ProductRequest productRequest) {
        Product product = new Product();
        updateProductFromRequest(product, productRequest);
        Product savedProduct = productRepository.save(product);
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
        List<Product> products = productRepository.findByActiveTrue();
        List<ProductResponse> productResponseList = new ArrayList<>();
        for(Product product : products) {
            ProductResponse productResponse = mapToProductResponse(product);
            productResponseList.add(productResponse);
        }
        return productResponseList;
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id).get();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(product.getId());
        productResponse.setName(product.getName());
        productResponse.setDescription(product.getDescription());
        productResponse.setImageUrl(product.getImageUrl());
        productResponse.setPrice(product.getPrice());
        productResponse.setCategory(product.getCategory());
        productResponse.setStockQuantity(product.getStockQuantity());
        productResponse.setActive(product.getActive());
        return productResponse;
    }

    public boolean updateProduct(Long id, ProductRequest productRequest) {
        return productRepository.findById(id)
                .map(existingProduct -> {
                    updateProductFromRequest(existingProduct, productRequest);
                    productRepository.save(existingProduct);
                    return true;
                }).orElse(false);
    }

    public Boolean deleteProduct(Long id) {
        if(productRepository.findById(id).isPresent()) {
            Product product = productRepository.findById(id).get();
            product.setActive(false);
            productRepository.save(product);
            return true;
        }
        return false;
    }

    public List<ProductResponse> searchProducts(String keyword) {
        List<Product> productList = productRepository.searchProducts(keyword);
        List<ProductResponse> productResponseList = new ArrayList<>();
        for(Product product : productList) {
            ProductResponse productResponse = mapToProductResponse(product);
            productResponseList.add(productResponse);
        }
        return productResponseList;
    }
}
