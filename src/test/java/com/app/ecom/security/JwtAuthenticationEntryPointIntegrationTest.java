package com.app.ecom.security;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationEntryPointIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequestReturnsSerializableApiErrorResponse() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/cart"))
                .andExpect(jsonPath("$.timestamp").isString())
                .andReturn();

        String timestamp = JsonPath.read(result.getResponse().getContentAsString(), "$.timestamp");
        assertThat(Instant.parse(timestamp)).isNotNull();
    }
}

