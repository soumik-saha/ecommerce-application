package com.app.ecom;

import com.app.ecom.model.User;
import com.app.ecom.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ApiEndpointsIntegrationTest {

    private static final String ADMIN_SECRET = "test-admin-secret";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Test
    void authEndpointsAreCovered() throws Exception {
        UserSession customer = registerAndLoginCustomer();

        // register + login are covered by helper assertions; logout is asserted here.
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void productEndpointsAreCovered() throws Exception {
        UserSession admin = registerAndLoginAdmin();
        UserSession customer = registerAndLoginCustomer();

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + customer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload("Unauthorized Product", 3)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        Long productId = createProductAsAdmin(admin.token(), "Auto Product", 15);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productId));

        mockMvc.perform(get("/api/products/search").param("keyword", "Auto"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload("Auto Product Updated", 8)))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void userEndpointsAreCovered() throws Exception {
        UserSession customer = registerAndLoginCustomer();

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());

        String managedEmail = "managed-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + customer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userPayload(managedEmail)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isOk());

        User managedUser = userRepository.findByEmail(managedEmail).orElseThrow();

        mockMvc.perform(get("/api/users/{id}", managedUser.getId())
                        .header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(managedEmail));

        String updatedEmail = "updated-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(put("/api/users/{id}", managedUser.getId())
                        .header("Authorization", "Bearer " + customer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userPayload(updatedEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("User updated"));
    }

    @Test
    void cartEndpointsAreCovered() throws Exception {
        UserSession admin = registerAndLoginAdmin();
        UserSession customer = registerAndLoginCustomer();
        Long productId = createProductAsAdmin(admin.token(), "Cart Product", 12);

        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + customer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":2}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cart")
                        .header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/cart/items/{productId}", productId)
                        .header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isNoContent());
    }

    @Test
    void orderEndpointsAreCovered() throws Exception {
        UserSession admin = registerAndLoginAdmin();
        UserSession customer = registerAndLoginCustomer();
        Long productId = createProductAsAdmin(admin.token(), "Order Product", 20);

        mockMvc.perform(post("/api/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/cart")
                        .header("Authorization", "Bearer " + customer.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":" + productId + ",\"quantity\":1}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists());

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + customer.token()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mockMvc.perform(get("/api/orders")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk());
    }

    @Test
    void auditLogBulkEndpointsAreCovered() throws Exception {
        UserSession admin = registerAndLoginAdmin();

        String idempotencyKey = "audit-" + UUID.randomUUID();
        String payload = "{" +
                "\"logs\":[" +
                "{" +
                "\"entityType\":\"PRODUCT\"," +
                "\"entityId\":1," +
                "\"action\":\"UPDATE\"," +
                "\"description\":\"Updated product price\"," +
                "\"oldValue\":\"999.00\"," +
                "\"newValue\":\"1299.00\"," +
                "\"idempotencyKey\":\"" + idempotencyKey + "\"" +
                "}," +
                "{" +
                "\"entityType\":\"PRODUCT\"," +
                "\"entityId\":1," +
                "\"action\":\"UPDATE\"," +
                "\"description\":\"Updated product price again\"," +
                "\"oldValue\":\"1299.00\"," +
                "\"newValue\":\"1499.00\"," +
                "\"idempotencyKey\":\"" + idempotencyKey + "\"" +
                "}" +
                "]}";

        mockMvc.perform(post("/api/audit-logs/batch")
                        .header("Authorization", "Bearer " + admin.token())
                        .header("X-Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProcessed").value(2))
                .andExpect(jsonPath("$.successCount").value(1))
                .andExpect(jsonPath("$.duplicateCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0));

        mockMvc.perform(get("/api/audit-logs/download")
                        .header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=audit-logs.csv"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.parseMediaType("text/csv")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("entityType")));
    }

    private UserSession registerAndLoginCustomer() throws Exception {
        String email = "customer-" + UUID.randomUUID() + "@example.com";
        String password = "Password@123";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        return login(email, password, "CUSTOMER");
    }

    private UserSession registerAndLoginAdmin() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@example.com";
        String password = "Password@123";

        mockMvc.perform(post("/api/auth/register/admin")
                        .header("X-Admin-Secret", ADMIN_SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerPayload(email, password)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        return login(email, password, "ADMIN");
    }

    private UserSession login(String email, String password, String expectedRole) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(expectedRole))
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();
        Long userId = body.get("userId").asLong();

        assertThat(token).isNotBlank();
        return new UserSession(token, userId);
    }

    private Long createProductAsAdmin(String adminToken, String name, int stockQuantity) throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productPayload(name, stockQuantity)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(createResult.getResponse().getContentAsString());
        return body.get("id").asLong();
    }

    private String registerPayload(String email, String password) {
        return "{" +
                "\"firstName\":\"Test\"," +
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

    private String userPayload(String email) {
        return "{" +
                "\"firstName\":\"Managed\"," +
                "\"lastName\":\"User\"," +
                "\"email\":\"" + email + "\"," +
                "\"phone\":\"9876543210\"," +
                "\"address\":{" +
                "\"street\":\"Sample Street\"," +
                "\"city\":\"Pune\"," +
                "\"state\":\"Maharashtra\"," +
                "\"zipcode\":\"411001\"," +
                "\"country\":\"India\"" +
                "}" +
                "}";
    }

    private String productPayload(String name, int stockQuantity) {
        return "{" +
                "\"name\":\"" + name + "\"," +
                "\"description\":\"Automated test product\"," +
                "\"price\":499.99," +
                "\"stockQuantity\":" + stockQuantity + "," +
                "\"category\":\"Electronics\"," +
                "\"imageUrl\":\"https://example.com/item.png\"" +
                "}";
    }

    private record UserSession(String token, Long userId) {
    }
}

