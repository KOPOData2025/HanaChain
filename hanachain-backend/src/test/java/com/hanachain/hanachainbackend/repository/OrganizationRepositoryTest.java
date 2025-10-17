package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.Organization;
import com.hanachain.hanachainbackend.entity.enums.OrganizationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

/**
 * Repository tests for Organization entity
 */
@DataJpaTest
@DisplayName("Organization Repository Tests")
class OrganizationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrganizationRepository organizationRepository;

    private Organization testOrganization1;
    private Organization testOrganization2;

    @BeforeEach
    void setUp() {
        testOrganization1 = Organization.builder()
            .name("Test Organization 1")
            .description("Test Description 1")
            .imageUrl("https://example.com/image1.jpg")
            .status(OrganizationStatus.ACTIVE)
            .build();

        testOrganization2 = Organization.builder()
            .name("Another Organization")
            .description("Another Description")
            .imageUrl("https://example.com/image2.jpg")
            .status(OrganizationStatus.INACTIVE)
            .build();

        entityManager.persistAndFlush(testOrganization1);
        entityManager.persistAndFlush(testOrganization2);
    }

    @Test
    @DisplayName("Should find organizations by status")
    void shouldFindOrganizationsByStatus() {
        // When
        Page<Organization> activeOrganizations = organizationRepository.findByStatus(
            OrganizationStatus.ACTIVE, PageRequest.of(0, 10));

        // Then
        assertThat(activeOrganizations.getContent()).hasSize(1);
        assertThat(activeOrganizations.getContent().get(0).getName()).isEqualTo("Test Organization 1");
        assertThat(activeOrganizations.getContent().get(0).getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should find organizations by name containing (case insensitive)")
    void shouldFindOrganizationsByNameContaining() {
        // When
        Page<Organization> organizations = organizationRepository.findByNameContainingIgnoreCase(
            "test", PageRequest.of(0, 10));

        // Then
        assertThat(organizations.getContent()).hasSize(1);
        assertThat(organizations.getContent().get(0).getName()).isEqualTo("Test Organization 1");
    }

    @Test
    @DisplayName("Should find organizations by status and name containing")
    void shouldFindOrganizationsByStatusAndNameContaining() {
        // When
        Page<Organization> organizations = organizationRepository.findByStatusAndNameContainingIgnoreCase(
            OrganizationStatus.ACTIVE, "organization", PageRequest.of(0, 10));

        // Then
        assertThat(organizations.getContent()).hasSize(1);
        assertThat(organizations.getContent().get(0).getName()).isEqualTo("Test Organization 1");
        assertThat(organizations.getContent().get(0).getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should count organizations by status")
    void shouldCountOrganizationsByStatus() {
        // When
        long activeCount = organizationRepository.countByStatus(OrganizationStatus.ACTIVE);
        long inactiveCount = organizationRepository.countByStatus(OrganizationStatus.INACTIVE);

        // Then
        assertThat(activeCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should check if organization name exists (case insensitive)")
    void shouldCheckIfOrganizationNameExists() {
        // When
        boolean existsExact = organizationRepository.existsByNameIgnoreCase("Test Organization 1");
        boolean existsCaseInsensitive = organizationRepository.existsByNameIgnoreCase("test organization 1");
        boolean notExists = organizationRepository.existsByNameIgnoreCase("Non-existent Organization");

        // Then
        assertThat(existsExact).isTrue();
        assertThat(existsCaseInsensitive).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("Should check if organization name exists excluding specific ID")
    void shouldCheckIfOrganizationNameExistsExcludingId() {
        // When
        boolean existsWithDifferentId = organizationRepository.existsByNameIgnoreCaseAndIdNot(
            "Test Organization 1", testOrganization2.getId());
        boolean notExistsWithSameId = organizationRepository.existsByNameIgnoreCaseAndIdNot(
            "Test Organization 1", testOrganization1.getId());

        // Then
        assertThat(existsWithDifferentId).isTrue();
        assertThat(notExistsWithSameId).isFalse();
    }

    @Test
    @DisplayName("Should find all active organizations")
    void shouldFindAllActiveOrganizations() {
        // When
        var activeOrganizations = organizationRepository.findAllActive();

        // Then
        assertThat(activeOrganizations).hasSize(1);
        assertThat(activeOrganizations.get(0).getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(activeOrganizations.get(0).getName()).isEqualTo("Test Organization 1");
    }

    @Test
    @DisplayName("Should find deletable organizations")
    void shouldFindDeletableOrganizations() {
        // When
        var deletableOrganizations = organizationRepository.findDeletableOrganizations();

        // Then
        // Since we haven't created any campaigns, both organizations should be deletable
        assertThat(deletableOrganizations).hasSize(2);
    }

    @Test
    @DisplayName("Should save and retrieve organization")
    void shouldSaveAndRetrieveOrganization() {
        // Given
        Organization newOrganization = Organization.builder()
            .name("New Test Organization")
            .description("New Test Description")
            .status(OrganizationStatus.ACTIVE)
            .build();

        // When
        Organization savedOrganization = organizationRepository.save(newOrganization);
        Organization retrievedOrganization = organizationRepository.findById(savedOrganization.getId()).orElse(null);

        // Then
        assertThat(savedOrganization).isNotNull();
        assertThat(savedOrganization.getId()).isNotNull();
        assertThat(retrievedOrganization).isNotNull();
        assertThat(retrievedOrganization.getName()).isEqualTo("New Test Organization");
        assertThat(retrievedOrganization.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should delete organization")
    void shouldDeleteOrganization() {
        // Given
        Long organizationId = testOrganization1.getId();

        // When
        organizationRepository.delete(testOrganization1);
        entityManager.flush();

        // Then
        var deletedOrganization = organizationRepository.findById(organizationId);
        assertThat(deletedOrganization).isEmpty();
    }

    @Test
    @DisplayName("Should update organization")
    void shouldUpdateOrganization() {
        // Given
        String newName = "Updated Organization Name";
        String newDescription = "Updated Description";

        // When
        testOrganization1.setName(newName);
        testOrganization1.setDescription(newDescription);
        testOrganization1.setStatus(OrganizationStatus.INACTIVE);
        
        Organization updatedOrganization = organizationRepository.save(testOrganization1);

        // Then
        assertThat(updatedOrganization.getName()).isEqualTo(newName);
        assertThat(updatedOrganization.getDescription()).isEqualTo(newDescription);
        assertThat(updatedOrganization.getStatus()).isEqualTo(OrganizationStatus.INACTIVE);
    }

    @Test
    @DisplayName("Should handle empty search results")
    void shouldHandleEmptySearchResults() {
        // When
        Page<Organization> emptyResults = organizationRepository.findByNameContainingIgnoreCase(
            "NonExistentOrganization", PageRequest.of(0, 10));

        // Then
        assertThat(emptyResults.getContent()).isEmpty();
        assertThat(emptyResults.getTotalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void shouldHandlePaginationCorrectly() {
        // Given - Create additional organizations for pagination test
        for (int i = 3; i <= 10; i++) {
            Organization org = Organization.builder()
                .name("Organization " + i)
                .description("Description " + i)
                .status(OrganizationStatus.ACTIVE)
                .build();
            entityManager.persistAndFlush(org);
        }

        // When
        Page<Organization> firstPage = organizationRepository.findByStatus(
            OrganizationStatus.ACTIVE, PageRequest.of(0, 5));
        Page<Organization> secondPage = organizationRepository.findByStatus(
            OrganizationStatus.ACTIVE, PageRequest.of(1, 5));

        // Then
        assertThat(firstPage.getContent()).hasSize(5);
        assertThat(secondPage.getContent()).hasSize(4); // 9 total active organizations
        assertThat(firstPage.getTotalElements()).isEqualTo(9);
        assertThat(secondPage.getTotalElements()).isEqualTo(9);
        assertThat(firstPage.hasNext()).isTrue();
        assertThat(secondPage.hasNext()).isFalse();
    }
}