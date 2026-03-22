package com.app.ecom.security;

import com.jayway.jsonpath.JsonPath;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAdminFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerAdminReturnsAdminRoleForValidSecret() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Admin-Secret", "test-admin-secret")
                        .content(registerPayload(email, "Password@123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.accessToken").isString());
    }

    @Test
    void registerAdminFailsForInvalidSecret() throws Exception {
        String email = "admin-invalid-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Admin-Secret", "wrong-secret")
                        .content(registerPayload(email, "Password@123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid admin registration secret"));
    }

    @Test
    void adminCanLoginAndLogoutWithBearerToken() throws Exception {
        String email = "admin-login-" + UUID.randomUUID() + "@example.com";
        String password = "Password@123";

        mockMvc.perform(post("/api/auth/register/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Admin-Secret", "test-admin-secret")
                        .content(registerPayload(email, password)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("accessToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void logoutRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotCreateProduct() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        String password = "Password@123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = loginJson.get("accessToken").asText();

        MvcResult createProductResult = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{" +
                                "\"name\":\"Restricted Product\"," +
                                "\"description\":\"Only admin can create this\"," +
                                "\"price\":199.99," +
                                "\"stockQuantity\":5," +
                                "\"category\":\"Test\"," +
                                "\"imageUrl\":\"https://example.com/p.png\"" +
                                "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("You do not have permission to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/products"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andReturn();

        String timestamp = JsonPath.read(createProductResult.getResponse().getContentAsString(), "$.timestamp");
        assertDoesNotThrow(() -> Instant.parse(timestamp));
    }

    private String registerPayload(String email, String password) {
        return "{" +
                "\"firstName\":\"Admin\"," +
                "\"lastName\":\"User\"," +
                "\"email\":\"" + email + "\"," +
                "\"phone\":\"9876543210\"," +
                "\"password\":\"" + password + "\"," +
                "\"address\":{" +
                "\"street\":\"MG Road\"," +
                "\"city\":\"Bengaluru\"," +
                "\"state\":\"Karnataka\"," +
                "\"zipcode\":\"560001\"," +
                "\"country\":\"India\"" +
                "}" +
                "}";
    }
}

