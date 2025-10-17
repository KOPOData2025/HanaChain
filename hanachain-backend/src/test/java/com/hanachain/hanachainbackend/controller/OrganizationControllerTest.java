package com.hanachain.hanachainbackend.controller;

import com.hanachain.hanachainbackend.controller.api.OrganizationController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hanachain.hanachainbackend.dto.organization.*;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import com.hanachain.hanachainbackend.exception.BusinessException;
import com.hanachain.hanachainbackend.exception.NotFoundException;
import com.hanachain.hanachainbackend.exception.ValidationException;
import com.hanachain.hanachainbackend.service.OrganizationService;
import com.hanachain.hanachainbackend.config.WebMvcTestConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OrganizationController
 */
@WebMvcTest(OrganizationController.class)
@Import(WebMvcTestConfiguration.class)
@ActiveProfiles({"test", "webmvc-test"})
@DisplayName("Organization Controller Integration Tests")
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrganizationService organizationService;

    private OrganizationResponse testOrganizationResponse;
    private OrganizationCreateRequest createRequestDTO;
    private OrganizationUpdateRequest updateRequestDTO;
    private OrganizationMemberResponse testMemberDTO;
    private OrganizationMemberAddRequest addMemberRequestDTO;
    private OrganizationMemberRoleUpdateRequest updateRoleRequestDTO;

    @BeforeEach
    void setUp() {
        // Test organization DTO setup
        testOrganizationResponse = OrganizationResponse.builder()
            .id(1L)
            .name("Test Organization")
            .description("Test Description")
            .imageUrl("https://example.com/image.jpg")
            .status(OrganizationStatus.ACTIVE)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .memberCount(5L)
            .adminCount(2L)
            .activeCampaignCount(3L)
            .build();

        // Test member DTO setup
        testMemberDTO = OrganizationMemberResponse.builder()
            .id(1L)
            .userId(1L)
            .username("testuser")
            .email("test@example.com")
            .role(OrganizationRole.ORG_ADMIN)
            .joinedAt(LocalDateTime.now())
            .active(true)
            .build();

        // Test request DTOs setup
        createRequestDTO = OrganizationCreateRequest.builder()
            .name("New Organization")
            .description("New Description")
            .imageUrl("https://example.com/new-image.jpg")
            .build();

        updateRequestDTO = OrganizationUpdateRequest.builder()
            .name("Updated Organization")
            .description("Updated Description")
            .imageUrl("https://example.com/updated-image.jpg")
            .status(OrganizationStatus.ACTIVE)
            .build();

        addMemberRequestDTO = OrganizationMemberAddRequest.builder()
            .userId(2L)
            .role(OrganizationRole.ORG_MEMBER)
            .build();

        updateRoleRequestDTO = OrganizationMemberRoleUpdateRequest.builder()
            .role(OrganizationRole.ORG_ADMIN)
            .build();
    }

    @Nested
    @DisplayName("Public Endpoint Tests")
    class PublicEndpointTests {

        @Test
        @DisplayName("GET /api/organizations - Should return paginated organizations")
        void shouldReturnPaginatedOrganizations() throws Exception {
            // Given
            Page<OrganizationResponse> organizationPage = new PageImpl<>(
                Arrays.asList(testOrganizationResponse), 
                PageRequest.of(0, 20), 
                1
            );
            when(organizationService.getAllOrganizations(any())).thenReturn(organizationPage);

            // When & Then
            mockMvc.perform(get("/api/organizations")
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id", is(1)))
                .andExpect(jsonPath("$.content[0].name", is("Test Organization")))
                .andExpect(jsonPath("$.totalElements", is(1)));

            verify(organizationService).getAllOrganizations(any());
        }

        @Test
        @DisplayName("GET /api/organizations/search - Should search organizations")
        void shouldSearchOrganizations() throws Exception {
            // Given
            Page<OrganizationResponse> searchResults = new PageImpl<>(
                Arrays.asList(testOrganizationResponse), 
                PageRequest.of(0, 20), 
                1
            );
            when(organizationService.searchOrganizations(eq("Test"), eq(OrganizationStatus.ACTIVE), any()))
                .thenReturn(searchResults);

            // When & Then
            mockMvc.perform(get("/api/organizations/search")
                    .param("name", "Test")
                    .param("status", "ACTIVE")
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name", is("Test Organization")));

            verify(organizationService).searchOrganizations(eq("Test"), eq(OrganizationStatus.ACTIVE), any());
        }

        @Test
        @DisplayName("GET /api/organizations/{id} - Should return organization by ID")
        void shouldReturnOrganizationById() throws Exception {
            // Given
            when(organizationService.getOrganizationById(1L)).thenReturn(testOrganizationResponse);

            // When & Then
            mockMvc.perform(get("/api/organizations/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Organization")))
                .andExpect(jsonPath("$.status", is("ACTIVE")));

            verify(organizationService).getOrganizationById(1L);
        }

        @Test
        @DisplayName("GET /api/organizations/{id} - Should return 404 when organization not found")
        void shouldReturn404WhenOrganizationNotFound() throws Exception {
            // Given
            when(organizationService.getOrganizationById(999L))
                .thenThrow(new NotFoundException("Organization not found: 999"));

            // When & Then
            mockMvc.perform(get("/api/organizations/999"))
                .andExpect(status().isNotFound());

            verify(organizationService).getOrganizationById(999L);
        }

        @Test
        @DisplayName("GET /api/organizations/{id}/details - Should return organization with details")
        void shouldReturnOrganizationWithDetails() throws Exception {
            // Given
            when(organizationService.getOrganizationByIdWithDetails(1L)).thenReturn(testOrganizationResponse);

            // When & Then
            mockMvc.perform(get("/api/organizations/1/details"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Test Organization")));

            verify(organizationService).getOrganizationByIdWithDetails(1L);
        }

        @Test
        @DisplayName("GET /api/organizations/{id}/members - Should return organization members")
        void shouldReturnOrganizationMembers() throws Exception {
            // Given
            Page<OrganizationMemberResponse> membersPage = new PageImpl<>(
                Arrays.asList(testMemberDTO), 
                PageRequest.of(0, 20), 
                1
            );
            when(organizationService.getOrganizationMembers(eq(1L), any())).thenReturn(membersPage);

            // When & Then
            mockMvc.perform(get("/api/organizations/1/members")
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId", is(1)))
                .andExpect(jsonPath("$.content[0].username", is("testuser")));

            verify(organizationService).getOrganizationMembers(eq(1L), any());
        }
    }

    @Nested
    @DisplayName("Authenticated User Endpoint Tests")
    class AuthenticatedUserEndpointTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /api/organizations - Should create organization successfully")
        void shouldCreateOrganizationSuccessfully() throws Exception {
            // Given
            when(organizationService.createOrganization(any(OrganizationCreateRequest.class), anyLong()))
                .thenReturn(testOrganizationResponse);

            // When & Then
            mockMvc.perform(post("/api/organizations")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Test Organization")));

            verify(organizationService).createOrganization(any(OrganizationCreateRequest.class), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /api/organizations - Should return 400 for invalid request")
        void shouldReturn400ForInvalidCreateRequest() throws Exception {
            // Given
            OrganizationCreateRequest invalidRequest = OrganizationCreateRequest.builder()
                .name("") // Invalid empty name
                .build();

            // When & Then
            mockMvc.perform(post("/api/organizations")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

            verify(organizationService, never()).createOrganization(any(), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /api/organizations - Should return 400 when name already exists")
        void shouldReturn400WhenNameAlreadyExists() throws Exception {
            // Given
            when(organizationService.createOrganization(any(OrganizationCreateRequest.class), anyLong()))
                .thenThrow(new ValidationException("Organization name already exists"));

            // When & Then
            mockMvc.perform(post("/api/organizations")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequestDTO)))
                .andExpect(status().isBadRequest());

            verify(organizationService).createOrganization(any(OrganizationCreateRequest.class), anyLong());
        }

        @Test
        @DisplayName("POST /api/organizations - Should return 401 when not authenticated")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequestDTO)))
                .andExpect(status().isUnauthorized());

            verify(organizationService, never()).createOrganization(any(), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/organizations/my-organizations - Should return user's organizations")
        void shouldReturnUserOrganizations() throws Exception {
            // Given
            List<OrganizationResponse> userOrganizations = Arrays.asList(testOrganizationResponse);
            when(organizationService.getOrganizationsForUser(anyLong())).thenReturn(userOrganizations);

            // When & Then
            mockMvc.perform(get("/api/organizations/my-organizations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test Organization")));

            verify(organizationService).getOrganizationsForUser(anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/organizations/my-admin-organizations - Should return user's admin organizations")
        void shouldReturnUserAdminOrganizations() throws Exception {
            // Given
            List<OrganizationResponse> adminOrganizations = Arrays.asList(testOrganizationResponse);
            when(organizationService.getOrganizationsWhereUserIsAdmin(anyLong())).thenReturn(adminOrganizations);

            // When & Then
            mockMvc.perform(get("/api/organizations/my-admin-organizations"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test Organization")));

            verify(organizationService).getOrganizationsWhereUserIsAdmin(anyLong());
        }
    }

    @Nested
    @DisplayName("Organization Admin Endpoint Tests")
    class OrganizationAdminEndpointTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("PUT /api/organizations/{id} - Should update organization successfully")
        void shouldUpdateOrganizationSuccessfully() throws Exception {
            // Given
            when(organizationService.updateOrganization(eq(1L), any(OrganizationUpdateRequest.class), anyLong()))
                .thenReturn(testOrganizationResponse);

            // When & Then
            mockMvc.perform(put("/api/organizations/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Test Organization")));

            verify(organizationService).updateOrganization(eq(1L), any(OrganizationUpdateRequest.class), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("PUT /api/organizations/{id} - Should return 403 when user is not admin")
        void shouldReturn403WhenUserIsNotAdmin() throws Exception {
            // Given
            when(organizationService.updateOrganization(eq(1L), any(OrganizationUpdateRequest.class), anyLong()))
                .thenThrow(new BusinessException("User does not have permission"));

            // When & Then
            mockMvc.perform(put("/api/organizations/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequestDTO)))
                .andExpect(status().isForbidden());

            verify(organizationService).updateOrganization(eq(1L), any(OrganizationUpdateRequest.class), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("DELETE /api/organizations/{id} - Should delete organization successfully")
        void shouldDeleteOrganizationSuccessfully() throws Exception {
            // Given
            doNothing().when(organizationService).deleteOrganization(eq(1L), anyLong());

            // When & Then
            mockMvc.perform(delete("/api/organizations/1")
                    .with(csrf()))
                .andExpect(status().isNoContent());

            verify(organizationService).deleteOrganization(eq(1L), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("DELETE /api/organizations/{id} - Should return 400 when organization cannot be deleted")
        void shouldReturn400WhenCannotBeDeleted() throws Exception {
            // Given
            doThrow(new BusinessException("Organization cannot be deleted as it has active campaigns"))
                .when(organizationService).deleteOrganization(eq(1L), anyLong());

            // When & Then
            mockMvc.perform(delete("/api/organizations/1")
                    .with(csrf()))
                .andExpect(status().isBadRequest());

            verify(organizationService).deleteOrganization(eq(1L), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("POST /api/organizations/{id}/members - Should add member successfully")
        void shouldAddMemberSuccessfully() throws Exception {
            // Given
            when(organizationService.addMemberToOrganization(eq(1L), any(OrganizationMemberAddRequest.class), anyLong()))
                .thenReturn(testMemberDTO);

            // When & Then
            mockMvc.perform(post("/api/organizations/1/members")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addMemberRequestDTO)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.username", is("testuser")));

            verify(organizationService).addMemberToOrganization(eq(1L), any(OrganizationMemberAddRequest.class), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("PUT /api/organizations/{id}/members/{userId}/role - Should update member role successfully")
        void shouldUpdateMemberRoleSuccessfully() throws Exception {
            // Given
            when(organizationService.updateMemberRole(eq(1L), eq(2L), any(OrganizationMemberRoleUpdateRequest.class), anyLong()))
                .thenReturn(testMemberDTO);

            // When & Then
            mockMvc.perform(put("/api/organizations/1/members/2/role")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRoleRequestDTO)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.role", is("ORG_ADMIN")));

            verify(organizationService).updateMemberRole(eq(1L), eq(2L), any(OrganizationMemberRoleUpdateRequest.class), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("DELETE /api/organizations/{id}/members/{userId} - Should remove member successfully")
        void shouldRemoveMemberSuccessfully() throws Exception {
            // Given
            doNothing().when(organizationService).removeMemberFromOrganization(eq(1L), eq(2L), anyLong());

            // When & Then
            mockMvc.perform(delete("/api/organizations/1/members/2")
                    .with(csrf()))
                .andExpect(status().isNoContent());

            verify(organizationService).removeMemberFromOrganization(eq(1L), eq(2L), anyLong());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/organizations/{id}/admins - Should return organization admins")
        void shouldReturnOrganizationAdmins() throws Exception {
            // Given
            List<OrganizationMemberResponse> admins = Arrays.asList(testMemberDTO);
            when(organizationService.isUserMemberOfOrganization(anyLong(), eq(1L))).thenReturn(true);
            when(organizationService.getOrganizationAdmins(eq(1L))).thenReturn(admins);

            // When & Then
            mockMvc.perform(get("/api/organizations/1/admins"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].role", is("ORG_ADMIN")));

            verify(organizationService).isUserMemberOfOrganization(anyLong(), eq(1L));
            verify(organizationService).getOrganizationAdmins(eq(1L));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/organizations/{id}/admins - Should return 403 when user is not member")
        void shouldReturn403WhenUserIsNotMemberForAdmins() throws Exception {
            // Given
            when(organizationService.isUserMemberOfOrganization(anyLong(), eq(1L))).thenReturn(false);

            // When & Then
            mockMvc.perform(get("/api/organizations/1/admins"))
                .andExpect(status().isForbidden());

            verify(organizationService).isUserMemberOfOrganization(anyLong(), eq(1L));
            verify(organizationService, never()).getOrganizationAdmins(any());
        }
    }

    @Nested
    @DisplayName("System Admin Endpoint Tests")
    class SystemAdminEndpointTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /api/organizations/admin/statistics - Should return organization statistics")
        void shouldReturnOrganizationStatistics() throws Exception {
            // Given
            when(organizationService.getOrganizationCount()).thenReturn(10L);
            when(organizationService.getActiveOrganizationCount()).thenReturn(8L);
            when(organizationService.getOrganizationsWithActiveCampaigns()).thenReturn(Arrays.asList(testOrganizationResponse));

            // When & Then
            mockMvc.perform(get("/api/organizations/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.totalOrganizations", is(10)))
                .andExpect(jsonPath("$.activeOrganizations", is(8)))
                .andExpect(jsonPath("$.organizationsWithActiveCampaigns", is(1)));

            verify(organizationService).getOrganizationCount();
            verify(organizationService).getActiveOrganizationCount();
            verify(organizationService).getOrganizationsWithActiveCampaigns();
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        @DisplayName("GET /api/organizations/admin/deletable - Should return deletable organizations")
        void shouldReturnDeletableOrganizations() throws Exception {
            // Given
            List<OrganizationResponse> deletableOrganizations = Arrays.asList(testOrganizationResponse);
            when(organizationService.getDeletableOrganizations()).thenReturn(deletableOrganizations);

            // When & Then
            mockMvc.perform(get("/api/organizations/admin/deletable"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test Organization")));

            verify(organizationService).getDeletableOrganizations();
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("GET /api/organizations/admin/statistics - Should return 403 for non-admin")
        void shouldReturn403ForNonAdminStatistics() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/organizations/admin/statistics"))
                .andExpect(status().isForbidden());

            verify(organizationService, never()).getOrganizationCount();
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle validation errors gracefully")
        void shouldHandleValidationErrorsGracefully() throws Exception {
            // Given
            OrganizationCreateRequest invalidRequest = OrganizationCreateRequest.builder()
                .name("") // Invalid empty name
                .description("A".repeat(2001)) // Too long description
                .build();

            // When & Then
            mockMvc.perform(post("/api/organizations")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJsonGracefully() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json"))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle missing content type gracefully")
        void shouldHandleMissingContentTypeGracefully() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/organizations")
                    .with(csrf())
                    .content(objectMapper.writeValueAsString(createRequestDTO)))
                .andExpect(status().isUnsupportedMediaType());
        }
    }
}