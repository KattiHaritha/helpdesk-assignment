package com.haritha.helpdesk.service;

import com.haritha.helpdesk.entity.Priority;
import com.haritha.helpdesk.entity.Status;
import com.haritha.helpdesk.entity.Ticket;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class TicketSpecifications {
    private TicketSpecifications() {}

    /** The organization predicate is ALWAYS added first: this is the tenant isolation for list queries. */
    public static Specification<Ticket> filter(Long orgId, Long requesterId, Status status, Priority priority,
                                               Long assignedAgentId, String text) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("organization").get("id"), orgId));
            if (requesterId != null) {
                predicates.add(cb.equal(root.get("requester").get("id"), requesterId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (assignedAgentId != null) {
                predicates.add(cb.equal(root.get("assignedAgent").get("id"), assignedAgentId));
            }
            if (text != null && !text.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("subject")), "%" + text.trim().toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
