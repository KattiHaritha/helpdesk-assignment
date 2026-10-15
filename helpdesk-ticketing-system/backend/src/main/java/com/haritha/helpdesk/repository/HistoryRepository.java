package com.haritha.helpdesk.repository;

import com.haritha.helpdesk.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryRepository extends JpaRepository<TicketHistory, Long> {
    List<TicketHistory> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
