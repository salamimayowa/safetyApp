package com.nigeria.health.bloodbank.entity;

import com.nigeria.health.shared.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Entity: User
 * Description: Core user account for every person on the platform.
 *              Implements UserDetails so Spring Security can use it directly.
 * Props: none (root entity)
 *
 * A user's ROLE determines what they can do:
 *  - CITIZEN / DONOR → can report accidents, verify drugs, search blood
 *  - HOSPITAL_ADMIN  → manages blood stock for their hospital
 *  - PHARMACY_ADMIN  → registers drugs for verification
 *  - FRSC_OFFICER    → manages accident reports
 *  - SUPER_ADMIN     → full access to everything
 */
@Entity
@Table(name = "users",
        uniqueConstraints = {
            @UniqueConstraint(columnNames = "email"),
            @UniqueConstraint(columnNames = "phone")
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String lga;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── UserDetails implementation ─────────────────────────────────
    // Spring Security uses these methods for authentication

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email; // we use email as the username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isVerified && isActive;
    }
}
