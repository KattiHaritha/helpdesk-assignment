package com.haritha.helpdesk;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.haritha.helpdesk.repository.UserRepository;
import com.haritha.helpdesk.security.JwtService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class TicketingApiIntegrationTest {
    private static final String PASSWORD = "IntegrationTest@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JwtService jwtService;

    private TestUsers users;

    @BeforeEach
    void createTestUsers() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String adminEmail = "admin-" + suffix + "@example.test";
        String customerEmail = "customer-" + suffix + "@example.test";
        String agentEmail = "agent-" + suffix + "@example.test";
        String adminToken = registerOrganization("Acme " + suffix, adminEmail);
        createUser(adminToken, "Customer", customerEmail, "CUSTOMER");
        createUser(adminToken, "Agent", agentEmail, "AGENT");
        users = new TestUsers(login(customerEmail), login(agentEmail));
    }

    @Test
    void customerSummaryMatchesOnlyTheirOwnTickets() throws Exception {
        createTicket(users.customerToken(), "Summary integration test");

        JsonNode summary = getJson("/api/tickets/summary", users.customerToken());
        JsonNode tickets = getJson("/api/tickets?size=100", users.customerToken());

        assertEquals(tickets.path("totalElements").asLong(), summary.path("total").asLong());
        assertEquals(1, summary.path("open").asLong());
    }

    @Test
    void customerCannotReadAnotherOrganizationsTicket() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        String otherAdminToken = registerOrganization("Globex " + suffix, "admin-" + suffix + "@example.test");
        long foreignTicketId = createTicket(otherAdminToken, "Another organization's ticket");

        mockMvc.perform(get("/api/tickets/{id}", foreignTicketId)
                        .header("Authorization", bearer(users.customerToken())))
                .andExpect(status().isNotFound());
    }

    @Test
    void internalNotesAreNotReturnedToCustomers() throws Exception {
        long ticketId = createTicket(users.customerToken(), "Internal note integration test");

        mockMvc.perform(post("/api/tickets/{id}/comments", ticketId)
                        .header("Authorization", bearer(users.agentToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"Internal integration note","internal":true}
                                """))
                .andExpect(status().isCreated());

        JsonNode customerComments = getJson("/api/tickets/" + ticketId + "/comments", users.customerToken());

        assertEquals(0, customerComments.size());
    }

    @Test
    void changingTokenVersionRevokesPreviouslyIssuedJwt() throws Exception {
        var principal = jwtService.parse(users.customerToken());
        var user = userRepository.findById(principal.userId()).orElseThrow();
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.saveAndFlush(user);

        mockMvc.perform(get("/api/tickets/summary")
                        .header("Authorization", bearer(users.customerToken())))
                .andExpect(status().isUnauthorized());
    }

    private String registerOrganization(String name, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/register-org")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("orgName", name)
                                .put("adminName", "Test Admin")
                                .put("email", email)
                                .put("password", PASSWORD)
                                .toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private void createUser(String adminToken, String name, String email, String role) throws Exception {
        mockMvc.perform(post("/api/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("name", name)
                                .put("email", email)
                                .put("password", PASSWORD)
                                .put("role", role)
                                .toString()))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("email", email)
                                .put("password", PASSWORD)
                                .toString()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
    }

    private long createTicket(String token, String subject) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/tickets")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.createObjectNode()
                                .put("subject", subject)
                                .put("description", "Created by API integration test")
                                .put("priority", "LOW")
                                .toString()))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private JsonNode getJson(String path, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private record TestUsers(String customerToken, String agentToken) {}
}
