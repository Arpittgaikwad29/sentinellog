package com.sentinellog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the REST API.
 *
 * Uses full Spring Boot context with MockMvc — no actual HTTP port needed.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LogControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /api/health returns UP status")
    void healthEndpoint() throws Exception {
        mvc.perform(get("/api/health"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$.status").value("UP"))
           .andExpect(jsonPath("$.version").value("1.0.0"));
    }

    @Test
    @DisplayName("GET /api/logs returns a JSON array")
    void logsEndpoint() throws Exception {
        mvc.perform(get("/api/logs?limit=10"))
           .andExpect(status().isOk())
           .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
           .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/logs/anomalies returns a JSON array")
    void anomaliesEndpoint() throws Exception {
        mvc.perform(get("/api/logs/anomalies"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/alerts returns a JSON array")
    void alertsEndpoint() throws Exception {
        mvc.perform(get("/api/alerts"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/stats returns expected fields")
    void statsEndpoint() throws Exception {
        mvc.perform(get("/api/stats"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.totalLogs").exists())
           .andExpect(jsonPath("$.anomalyCount").exists())
           .andExpect(jsonPath("$.anomalyRate").exists());
    }
}
