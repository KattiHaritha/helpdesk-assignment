package com.haritha.helpdesk.repository;

import com.haritha.helpdesk.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
}
