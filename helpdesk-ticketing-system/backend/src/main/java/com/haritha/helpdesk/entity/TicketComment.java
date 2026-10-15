package com.haritha.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ticket_comments")
@Getter @Setter @NoArgsConstructor
public class TicketComment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 4000)
    private String content;

    // internal agent note: never shown to customers
    @Column(name = "is_internal", nullable = false)
    private boolean internal;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
