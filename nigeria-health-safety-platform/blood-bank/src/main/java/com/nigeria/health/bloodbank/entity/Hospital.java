package com.nigeria.health.bloodbank.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity: Hospital
 * Description: A registered hospital on the blood bank network.
 *              Must be approved by SUPER_ADMIN before appearing in searches.
 *              Contains location data (state, LGA, lat/lng) for proximity matching.
 * Props: none
 */
@Entity
@Table(name = "hospitals")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(nullable = false, length = 50)
    private String lga;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(precision = 10, scale = 8)
    private Double latitude;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(precision = 11, scale = 8)
    private Double longitude;

    @Column(nullable = false, length = 150)
    private String contactEmail;

    @Column(nullable = false, length = 20)
    private String contactPhone;

    // The HOSPITAL_ADMIN user account linked to this hospital
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id")
    private User adminUser;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    private LocalDateTime approvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    // Blood stock levels — one record per blood type
    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<BloodStock> bloodStock = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}