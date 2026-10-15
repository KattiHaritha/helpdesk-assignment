package com.haritha.helpdesk.repository;

import com.haritha.helpdesk.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<TicketComment, Long> {
    List<TicketComment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    List<TicketComment> findByTicketIdAndInternalFalseOrderByCreatedAtAsc(Long ticketId);
}
