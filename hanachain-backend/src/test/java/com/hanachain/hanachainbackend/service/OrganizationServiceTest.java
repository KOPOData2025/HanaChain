package com.hanachain.hanachainbackend.service;

import com.hanachain.hanachainbackend.dto.organization.*;
import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.OrganizationUser;
import com.hanachain.hanachainbackend.entity.User;
import com.hanachain.hanachainbackend.entity.enums.OrganizationRole;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import com.hanachain.hanachainbackend.exception.BusinessException;
import com.hanachain.hanachainbackend.exception.NotFoundException;
import com.hanachain.hanachainbackend.exception.ValidationException;
import com.hanachain.hanachainbackend.repository.OrganizationRepository;
import com.hanachain.hanachainbackend.repository.OrganizationUserRepository;
import com.hanachain.hanachainbackend.repository.UserRepository;
import com.hanachain.hanachainbackend.service.impl.OrganizationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrganizationService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Organization Service Tests")
class OrganizationServiceTest {

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OrganizationUserRepository organizationUserRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    private Organization testOrganization;
    private User testUser;
    private OrganizationUser testOrgUser;
    private OrganizationCreateRequest createRequestDTO;
    private OrganizationUpdateRequest updateRequestDTO;

    @BeforeEach
    void setUp() {
        // Test user setup
        testUser = User.builder()
            .id(1L)
            .name("testuser")
            .email("test@example.com")
            .enabled(true)
            .build();

        // Test organization setup
        testOrganization = Organization.builder()
            .id(1L)
            .name("Test Organization")
            .description("Test Description")
            .imageUrl("https://example.com/image.jpg")
            .status(OrganizationStatus.ACTIVE)
            .build();

        // Test organization user setup
        testOrgUser = OrganizationUser.builder()
            .id(1L)
            .organization(testOrganization)
            .user(testUser)
            .role(OrganizationRole.ORG_ADMIN)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();

        // Test DTOs setup
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
    }

    @Nested
    @DisplayName("Organization Creation Tests")
    class OrganizationCreationTests {

        @Test
        @DisplayName("Should create organization successfully")
        void shouldCreateOrganizationSuccessfully() {
            // Given
            when(organizationRepository.existsByNameIgnoreCase(createRequestDTO.getName())).thenReturn(false);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
            when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

            // When
            OrganizationResponse result = organizationService.createOrganization(createRequestDTO, testUser.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo(testOrganization.getName());
            assertThat(result.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);

            verify(organizationRepository).existsByNameIgnoreCase(createRequestDTO.getName());
            verify(userRepository).findById(testUser.getId());
            verify(organizationRepository, times(2)).save(any(Organization.class));
        }

        @Test
        @DisplayName("Should throw ValidationException when organization name already exists")
        void shouldThrowValidationExceptionWhenNameExists() {
            // Given
            when(organizationRepository.existsByNameIgnoreCase(createRequestDTO.getName())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> organizationService.createOrganization(createRequestDTO, testUser.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Organization name already exists");

            verify(organizationRepository).existsByNameIgnoreCase(createRequestDTO.getName());
            verify(organizationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw NotFoundException when creator user not found")
        void shouldThrowNotFoundExceptionWhenCreatorNotFound() {
            // Given
            when(organizationRepository.existsByNameIgnoreCase(createRequestDTO.getName())).thenReturn(false);
            when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationService.createOrganization(createRequestDTO, testUser.getId()))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("User not found");

            verify(organizationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Organization Retrieval Tests")
    class OrganizationRetrievalTests {

        @Test
        @DisplayName("Should get organization by ID successfully")
        void shouldGetOrganizationByIdSuccessfully() {
            // Given
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));

            // When
            OrganizationResponse result = organizationService.getOrganizationById(testOrganization.getId());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(testOrganization.getId());
            assertThat(result.getName()).isEqualTo(testOrganization.getName());

            verify(organizationRepository).findById(testOrganization.getId());
        }

        @Test
        @DisplayName("Should throw NotFoundException when organization not found")
        void shouldThrowNotFoundExceptionWhenOrganizationNotFound() {
            // Given
            when(organizationRepository.findById(anyLong())).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> organizationService.getOrganizationById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Organization not found");
        }

        @Test
        @DisplayName("Should get all organizations with pagination")
        void shouldGetAllOrganizationsWithPagination() {
            // Given
            Pageable pageable = PageRequest.of(0, 20);
            List<Organization> organizations = Arrays.asList(testOrganization);
            Page<Organization> organizationPage = new PageImpl<>(organizations, pageable, 1);

            when(organizationRepository.findAll(pageable)).thenReturn(organizationPage);

            // When
            Page<OrganizationResponse> result = organizationService.getAllOrganizations(pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo(testOrganization.getName());

            verify(organizationRepository).findAll(pageable);
        }

        @Test
        @DisplayName("Should search organizations by name and status")
        void shouldSearchOrganizationsByNameAndStatus() {
            // Given
            String searchName = "Test";
            OrganizationStatus status = OrganizationStatus.ACTIVE;
            Pageable pageable = PageRequest.of(0, 20);
            List<Organization> organizations = Arrays.asList(testOrganization);
            Page<Organization> organizationPage = new PageImpl<>(organizations, pageable, 1);

            when(organizationRepository.findByStatusAndNameContainingIgnoreCase(status, searchName, pageable))
                .thenReturn(organizationPage);

            // When
            Page<OrganizationResponse> result = organizationService.searchOrganizations(searchName, status, pageable);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);

            verify(organizationRepository).findByStatusAndNameContainingIgnoreCase(status, searchName, pageable);
        }
    }

    @Nested
    @DisplayName("Organization Update Tests")
    class OrganizationUpdateTests {

        @Test
        @DisplayName("Should update organization successfully")
        void shouldUpdateOrganizationSuccessfully() {
            // Given
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);
            when(organizationRepository.existsByNameIgnoreCaseAndIdNot(updateRequestDTO.getName(), testOrganization.getId()))
                .thenReturn(false);
            when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);

            // When
            OrganizationResponse result = organizationService.updateOrganization(
                testOrganization.getId(), updateRequestDTO, testUser.getId());

            // Then
            assertThat(result).isNotNull();
            verify(organizationRepository).save(any(Organization.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when user is not admin")
        void shouldThrowBusinessExceptionWhenUserIsNotAdmin() {
            // Given
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> organizationService.updateOrganization(
                testOrganization.getId(), updateRequestDTO, testUser.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not have permission");

            verify(organizationRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ValidationException when updated name already exists")
        void shouldThrowValidationExceptionWhenUpdatedNameExists() {
            // Given
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);
            when(organizationRepository.existsByNameIgnoreCaseAndIdNot(updateRequestDTO.getName(), testOrganization.getId()))
                .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> organizationService.updateOrganization(
                testOrganization.getId(), updateRequestDTO, testUser.getId()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Organization name already exists");

            verify(organizationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Organization Deletion Tests")
    class OrganizationDeletionTests {

        @Test
        @DisplayName("Should delete organization successfully")
        void shouldDeleteOrganizationSuccessfully() {
            // Given
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);
            when(testOrganization.canBeDeleted()).thenReturn(true);

            // When
            organizationService.deleteOrganization(testOrganization.getId(), testUser.getId());

            // Then
            verify(organizationRepository).delete(testOrganization);
        }

        @Test
        @DisplayName("Should throw BusinessException when user is not admin")
        void shouldThrowBusinessExceptionWhenUserIsNotAdminForDeletion() {
            // Given
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> organizationService.deleteOrganization(testOrganization.getId(), testUser.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("does not have permission");

            verify(organizationRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw BusinessException when organization cannot be deleted")
        void shouldThrowBusinessExceptionWhenCannotBeDeleted() {
            // Given
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);
            when(testOrganization.canBeDeleted()).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> organizationService.deleteOrganization(testOrganization.getId(), testUser.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be deleted as it has active campaigns");

            verify(organizationRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Member Management Tests")
    class MemberManagementTests {

        @Test
        @DisplayName("Should add member to organization successfully")
        void shouldAddMemberToOrganizationSuccessfully() {
            // Given
            OrganizationMemberAddRequest request = OrganizationMemberAddRequest.builder()
                .userId(2L)
                .role(OrganizationRole.ORG_MEMBER)
                .build();

            User newUser = User.builder().id(2L).name("newuser").email("new@example.com").build();

            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));
            when(userRepository.findById(2L)).thenReturn(Optional.of(newUser));
            when(organizationUserRepository.existsByOrganizationIdAndUserId(testOrganization.getId(), 2L))
                .thenReturn(false);
            when(organizationUserRepository.save(any(OrganizationUser.class))).thenReturn(testOrgUser);

            // When
            OrganizationMemberResponse result = organizationService.addMemberToOrganization(
                testOrganization.getId(), request, testUser.getId());

            // Then
            assertThat(result).isNotNull();
            verify(organizationUserRepository).save(any(OrganizationUser.class));
        }

        @Test
        @DisplayName("Should throw BusinessException when user is already a member")
        void shouldThrowBusinessExceptionWhenUserAlreadyMember() {
            // Given
            OrganizationMemberAddRequest request = OrganizationMemberAddRequest.builder()
                .userId(2L)
                .role(OrganizationRole.ORG_MEMBER)
                .build();

            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);
            when(organizationRepository.findById(testOrganization.getId())).thenReturn(Optional.of(testOrganization));
            when(userRepository.findById(2L)).thenReturn(Optional.of(testUser));
            when(organizationUserRepository.existsByOrganizationIdAndUserId(testOrganization.getId(), 2L))
                .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> organizationService.addMemberToOrganization(
                testOrganization.getId(), request, testUser.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already a member");

            verify(organizationUserRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should remove member from organization successfully")
        void shouldRemoveMemberFromOrganizationSuccessfully() {
            // Given
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);
            when(organizationUserRepository.findByOrganizationIdAndUserId(testOrganization.getId(), 2L))
                .thenReturn(Optional.of(testOrgUser));
            when(organizationUserRepository.countByOrganizationIdAndRole(testOrganization.getId(), OrganizationRole.ORG_ADMIN))
                .thenReturn(2L); // More than 1 admin

            testOrgUser.setRole(OrganizationRole.ORG_MEMBER); // Not admin, so can be removed

            // When
            organizationService.removeMemberFromOrganization(testOrganization.getId(), 2L, testUser.getId());

            // Then
            verify(organizationUserRepository).delete(testOrgUser);
        }

        @Test
        @DisplayName("Should throw BusinessException when trying to remove last admin")
        void shouldThrowBusinessExceptionWhenRemovingLastAdmin() {
            // Given
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);
            when(organizationUserRepository.findByOrganizationIdAndUserId(testOrganization.getId(), 2L))
                .thenReturn(Optional.of(testOrgUser));
            when(organizationUserRepository.countByOrganizationIdAndRole(testOrganization.getId(), OrganizationRole.ORG_ADMIN))
                .thenReturn(1L); // Only 1 admin

            testOrgUser.setRole(OrganizationRole.ORG_ADMIN); // Is admin

            // When & Then
            assertThatThrownBy(() -> organizationService.removeMemberFromOrganization(
                testOrganization.getId(), 2L, testUser.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot remove the last admin");

            verify(organizationUserRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Validation and Business Logic Tests")
    class ValidationAndBusinessLogicTests {

        @Test
        @DisplayName("Should check if organization name exists")
        void shouldCheckIfOrganizationNameExists() {
            // Given
            when(organizationRepository.existsByNameIgnoreCase("Test Organization")).thenReturn(true);

            // When
            boolean exists = organizationService.existsByName("Test Organization");

            // Then
            assertThat(exists).isTrue();
            verify(organizationRepository).existsByNameIgnoreCase("Test Organization");
        }

        @Test
        @DisplayName("Should check if user is admin of organization")
        void shouldCheckIfUserIsAdminOfOrganization() {
            // Given
            when(organizationUserRepository.isUserAdminOfOrganization(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);

            // When
            boolean isAdmin = organizationService.isUserAdminOfOrganization(testUser.getId(), testOrganization.getId());

            // Then
            assertThat(isAdmin).isTrue();
            verify(organizationUserRepository).isUserAdminOfOrganization(testOrganization.getId(), testUser.getId());
        }

        @Test
        @DisplayName("Should check if user is member of organization")
        void shouldCheckIfUserIsMemberOfOrganization() {
            // Given
            when(organizationUserRepository.existsByOrganizationIdAndUserId(testOrganization.getId(), testUser.getId()))
                .thenReturn(true);

            // When
            boolean isMember = organizationService.isUserMemberOfOrganization(testUser.getId(), testOrganization.getId());

            // Then
            assertThat(isMember).isTrue();
            verify(organizationUserRepository).existsByOrganizationIdAndUserId(testOrganization.getId(), testUser.getId());
        }

        @Test
        @DisplayName("Should get organization count")
        void shouldGetOrganizationCount() {
            // Given
            when(organizationRepository.count()).thenReturn(10L);

            // When
            long count = organizationService.getOrganizationCount();

            // Then
            assertThat(count).isEqualTo(10L);
            verify(organizationRepository).count();
        }

        @Test
        @DisplayName("Should get active organization count")
        void shouldGetActiveOrganizationCount() {
            // Given
            when(organizationRepository.countByStatus(OrganizationStatus.ACTIVE)).thenReturn(8L);

            // When
            long count = organizationService.getActiveOrganizationCount();

            // Then
            assertThat(count).isEqualTo(8L);
            verify(organizationRepository).countByStatus(OrganizationStatus.ACTIVE);
        }
    }
}