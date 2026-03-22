package com.app.ecom.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    @Pattern(
            regexp = "^(\\+91[- ]?)?[6-9]\\d{9}$",
            message = "Invalid Indian phone number"
    )
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Address is required")
    @Valid
    private AddressDTO address;
}

