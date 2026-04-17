package com.app.ecom.controller;

import com.app.ecom.dto.ProductRequest;
import com.app.ecom.dto.ProductResponse;
import com.app.ecom.dto.ReviewResponse;
import com.app.ecom.service.ProductService;
import com.app.ecom.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Validated
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductRequest productRequest) {
        log.info("Create product request received for name={}", productRequest.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(productRequest));
    }

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must be at most 100") int size) {
        log.info("Fetching products with keyword='{}', page={}, size={}", keyword, page, size);
        return ResponseEntity.ok(productService.getProducts(keyword, page, size));
    }

    /*@GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        return new ResponseEntity<List<ProductResponse>>(productService.getAllProducts(), HttpStatus.OK);
    }*/

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable @Positive(message = "id must be positive") Long id) {
        log.info("Fetching product by id={}", id);
        return ResponseEntity.ok(productService.getProductResponseById(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getProductReviews(
            @PathVariable @Positive(message = "id must be positive") Long id,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size must be at least 1")
            @Max(value = 50, message = "size must be at most 50") int size) {
        log.info("Fetching reviews for productId={}, page={}, size={}", id, page, size);
        return ResponseEntity.ok(reviewService.getReviewsByProduct(id, page, size));
    }

    @PutMapping("/{id}")
    public ResponseEntity<String> updateProduct(@PathVariable @Positive(message = "id must be positive") Long id,
                                                @Valid @RequestBody ProductRequest productRequest) {
        log.info("Update product request received for id={}", id);
        productService.updateProduct(id, productRequest);
        log.info("Product updated successfully for id={}", id);
        return ResponseEntity.ok("Product updated successfully");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable @Positive(message = "id must be positive") Long id) {
        log.info("Delete product request received for id={}", id);
        productService.deleteProduct(id);
        log.info("Product soft deleted for id={}", id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<ProductResponse>> searchProducts(@RequestParam @NotBlank(message = "keyword is required") String keyword) {
        log.info("Product search request received with keyword='{}'", keyword);
        return ResponseEntity.ok(productService.searchProducts(keyword));
    }

    @GetMapping(value = "/search", params = "q")
    public ResponseEntity<Page<ProductResponse>> searchProductsPaged(
            @RequestParam("q") @NotBlank(message = "q is required") String keyword,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page must be 0 or greater") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "limit must be at least 1")
            @Max(value = 100, message = "limit must be at most 100") int limit) {
        log.info("Product search paged request received with keyword='{}', page={}, limit={}", keyword, page, limit);
        return ResponseEntity.ok(productService.searchProductsPaged(keyword, page, limit));
    }
}
