package com.nigeria.health.bloodbank.entity;

import com.nigeria.health.shared.enums.BloodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity: BloodStock
 * Description: Tracks how many units of each blood type a hospital currently has.
 *              Each hospital has one BloodStock record per blood type (8 records max).
 *              When units drop below 2, a low-stock email is triggered.
 * Props: none
 */
@Entity
@Table(name = "blood_stock",
        uniqueConstraints = @UniqueConstraint(columnNames = {"hospital_id", "blood_type"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BloodType bloodType;

    @Column(nullable = false)
    @Builder.Default
    private Integer unitsAvailable = 0;

    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    // ─── Business logic ────────────────────────────────────────────

    public boolean isLow() {
        return unitsAvailable < 2;
    }

    public boolean hasAvailableUnits(int required) {
        return unitsAvailable >= required;
    }
}
