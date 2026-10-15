package com.haritha.helpdesk.repository;

import com.haritha.helpdesk.entity.Role;
import com.haritha.helpdesk.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    // tenant-scoped lookups
    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);

    List<User> findByOrganizationIdOrderByNameAsc(Long organizationId);

    List<User> findByOrganizationIdAndRoleOrderByNameAsc(Long organizationId, Role role);
}
