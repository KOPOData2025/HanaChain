package com.hanachain.hanachainbackend.repository;

import com.hanachain.hanachainbackend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsByName(String name);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.enabled = true")
    Optional<User> findByEmailAndEnabled(@Param("email") String email);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'USER'")
    long countUsers();
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = 'ADMIN'")
    long countAdmins();
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND (" +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
           ") ORDER BY u.name ASC")
    List<User> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.role = 'USER' AND (" +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))" +
           ") ORDER BY u.name ASC")
    Page<User> searchByKeywordPage(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.enabled = true AND u.role = :role ORDER BY u.name ASC")
    Page<User> findByRole(@Param("role") User.Role role, Pageable pageable);
    
    List<User> findByRoleNot(User.Role role);
}
