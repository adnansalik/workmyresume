package io.github.adnansaliq.workmyresume.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id",nullable = false)
    private Long id;
    @Column(name="email",nullable = false,unique = true)
    private String email;
    @Column(name="password",nullable = false)
    private String password;
    @Column(name = "is_active")
    private Boolean isActive=true;
    @Column(name = "created_at",updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.ROLE_USER;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Plan plan = Plan.FREE;

}

