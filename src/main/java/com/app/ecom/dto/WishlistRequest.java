package com.app.ecom.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WishlistRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;
}
