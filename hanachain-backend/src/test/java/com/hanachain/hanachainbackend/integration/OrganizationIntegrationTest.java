package com.hanachain.hanachainbackend.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanachain.hanachainbackend.dto.organization.OrganizationCreateRequest;
import com.hanachain.hanachainbackend.dto.organization.OrganizationUpdateRequest;
import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import com.hanachain.hanachainbackend.repository.OrganizationRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Organization management with full Spring context
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("integration-test")
@Transactional
@DisplayName("Organization Integration Tests")
class OrganizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private Organization testOrganization;
    private OrganizationCreateRequest createRequestDTO;
    private OrganizationUpdateRequest updateRequestDTO;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        organizationRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
            .name("testuser")
            .email("test@example.com")
            .password("encodedPassword")
            .enabled(true)
            .build();
        testUser = userRepository.save(testUser);

        // Create test organization
        testOrganization = Organization.builder()
            .name("Integration Test Organization")
            .description("Test Description")
            .imageUrl("https://example.com/test.jpg")
            .status(OrganizationStatus.ACTIVE)
            .build();
        testOrganization = organizationRepository.save(testOrganization);

        // Setup DTOs
        createRequestDTO = OrganizationCreateRequest.builder()
            .name("New Integration Test Organization")
            .description("New Test Description")
            .imageUrl("https://example.com/new-test.jpg")
            .build();

        updateRequestDTO = OrganizationUpdateRequest.builder()
            .name("Updated Integration Test Organization")
            .description("Updated Test Description")
            .imageUrl("https://example.com/updated-test.jpg")
            .status(OrganizationStatus.ACTIVE)
            .build();
    }

    @Test
    @DisplayName("Full organization lifecycle test")
    @WithMockUser(roles = "USER")
    void shouldCompleteFullOrganizationLifecycle() throws Exception {
        // 1. Create organization
        String createResponse = mockMvc.perform(post("/api/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDTO)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is("New Integration Test Organization")))
            .andExpect(jsonPath("$.status", is("ACTIVE")))
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Extract organization ID from response
        var createdOrg = objectMapper.readTree(createResponse);
        Long orgId = createdOrg.get("id").asLong();

        // 2. Verify organization exists in database
        var dbOrganization = organizationRepository.findById(orgId);
        assert dbOrganization.isPresent();
        assert dbOrganization.get().getName().equals("New Integration Test Organization");

        // 3. Get organization details
        mockMvc.perform(get("/api/organizations/" + orgId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(orgId.intValue())))
            .andExpect(jsonPath("$.name", is("New Integration Test Organization")));

        // 4. Update organization
        mockMvc.perform(put("/api/organizations/" + orgId)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequestDTO)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Updated Integration Test Organization")));

        // 5. Verify update in database
        var updatedDbOrg = organizationRepository.findById(orgId);
        assert updatedDbOrg.isPresent();
        assert updatedDbOrg.get().getName().equals("Updated Integration Test Organization");

        // 6. Delete organization
        mockMvc.perform(delete("/api/organizations/" + orgId)
                .with(csrf()))
            .andExpect(status().isNoContent());

        // 7. Verify soft delete
        var deletedDbOrg = organizationRepository.findById(orgId);
        assert deletedDbOrg.isEmpty(); // Should be empty due to soft delete
    }

    @Test
    @DisplayName("Public endpoints accessibility test")
    void shouldAccessPublicEndpointsWithoutAuthentication() throws Exception {
        // Test pagination
        mockMvc.perform(get("/api/organizations")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").isNumber());

        // Test search
        mockMvc.perform(get("/api/organizations/search")
                .param("name", "test")
                .param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());

        // Test organization details
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(testOrganization.getId().intValue())))
            .andExpect(jsonPath("$.name", is("Integration Test Organization")));

        // Test organization members
        mockMvc.perform(get("/api/organizations/" + testOrganization.getId() + "/members"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Authentication and authorization test")
    void shouldEnforceAuthenticationAndAuthorization() throws Exception {
        // Test unauthorized access to protected endpoints
        mockMvc.perform(post("/api/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDTO)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/organizations/" + testOrganization.getId())
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequestDTO)))
            .andExpect(status().isUnauthorized());

        mockMvc.perform(delete("/api/organizations/" + testOrganization.getId())
                .with(csrf()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Data validation test")
    @WithMockUser(roles = "USER")
    void shouldValidateRequestData() throws Exception {
        // Test empty name validation
        OrganizationCreateRequest invalidRequest = OrganizationCreateRequest.builder()
            .name("") // Invalid empty name
            .description("Valid description")
            .build();

        mockMvc.perform(post("/api/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
            .andExpect(status().isBadRequest());

        // Test too long description
        OrganizationCreateRequest longDescRequest = OrganizationCreateRequest.builder()
            .name("Valid Name")
            .description("A".repeat(2001)) // Too long
            .build();

        mockMvc.perform(post("/api/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(longDescRequest)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Search functionality test")
    void shouldPerformSearchCorrectly() throws Exception {
        // Create additional test organizations for search
        Organization org1 = Organization.builder()
            .name("Search Test Organization Alpha")
            .description("Alpha description")
            .status(OrganizationStatus.ACTIVE)
            .build();
        organizationRepository.save(org1);

        Organization org2 = Organization.builder()
            .name("Search Test Organization Beta")
            .description("Beta description")
            .status(OrganizationStatus.INACTIVE)
            .build();
        organizationRepository.save(org2);

        // Test search by name
        mockMvc.perform(get("/api/organizations/search")
                .param("name", "Search Test"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(2))));

        // Test search by status
        mockMvc.perform(get("/api/organizations/search")
                .param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[*].status", everyItem(is("ACTIVE"))));

        // Test search by name and status
        mockMvc.perform(get("/api/organizations/search")
                .param("name", "Search Test")
                .param("status", "ACTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].name", containsString("Alpha")));
    }

    @Test
    @DisplayName("Error handling test")
    void shouldHandleErrorsGracefully() throws Exception {
        // Test non-existent organization
        mockMvc.perform(get("/api/organizations/99999"))
            .andExpect(status().isNotFound());

        // Test malformed JSON
        mockMvc.perform(post("/api/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
            .andExpect(status().isBadRequest());

        // Test unsupported media type
        mockMvc.perform(post("/api/organizations")
                .with(csrf())
                .content(objectMapper.writeValueAsString(createRequestDTO)))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Pagination test")
    void shouldHandlePaginationCorrectly() throws Exception {
        // Create multiple organizations for pagination test
        for (int i = 1; i <= 15; i++) {
            Organization org = Organization.builder()
                .name("Pagination Test Organization " + i)
                .description("Description " + i)
                .status(OrganizationStatus.ACTIVE)
                .build();
            organizationRepository.save(org);
        }

        // Test first page
        mockMvc.perform(get("/api/organizations")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(10)))
            .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(15)))
            .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(2)))
            .andExpect(jsonPath("$.first", is(true)))
            .andExpect(jsonPath("$.last", is(false)));

        // Test second page
        mockMvc.perform(get("/api/organizations")
                .param("page", "1")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(5))))
            .andExpect(jsonPath("$.first", is(false)));
    }

    @Test
    @DisplayName("Database transaction test")
    @WithMockUser(roles = "USER")
    void shouldMaintainDataConsistency() throws Exception {
        // Count organizations before
        long initialCount = organizationRepository.count();

        // Create organization
        String response = mockMvc.perform(post("/api/organizations")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequestDTO)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Verify count increased
        long afterCreateCount = organizationRepository.count();
        assert afterCreateCount == initialCount + 1;

        // Extract organization ID
        var createdOrg = objectMapper.readTree(response);
        Long orgId = createdOrg.get("id").asLong();

        // Delete organization
        mockMvc.perform(delete("/api/organizations/" + orgId)
                .with(csrf()))
            .andExpect(status().isNoContent());

        // Verify count decreased (soft delete)
        long afterDeleteCount = organizationRepository.count();
        assert afterDeleteCount == initialCount; // Should be back to initial count
    }
}