package com.haritha.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, columnDefinition = "bigint default 0")
    private long tokenVersion = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // Every user belongs to exactly one organization (tenant)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
