package com.haritha.helpdesk.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "organizations")
@Getter @Setter @NoArgsConstructor
public class Organization {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
