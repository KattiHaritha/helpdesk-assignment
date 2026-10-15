package com.haritha.helpdesk.repository;

import com.haritha.helpdesk.entity.Status;
import com.haritha.helpdesk.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    // tenant-scoped lookup: a ticket from another org is simply "not found"
    Optional<Ticket> findByIdAndOrganizationId(Long id, Long organizationId);

    @Query("""
            select count(t) from Ticket t
            where t.organization.id = :organizationId
              and (:requesterId is null or t.requester.id = :requesterId)
              and (:status is null or t.status = :status)
            """)
    long countVisibleByOrganizationAndRequesterAndStatus(@Param("organizationId") Long organizationId,
                                                          @Param("requesterId") Long requesterId,
                                                          @Param("status") Status status);
}
