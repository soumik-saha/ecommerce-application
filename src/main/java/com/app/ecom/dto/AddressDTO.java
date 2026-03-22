package com.app.ecom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressDTO {

    @NotBlank(message = "Street is required")
    private String street;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Zipcode is required")
    private String zipcode;

    @NotBlank(message = "Country is required")
    private String country;
}
