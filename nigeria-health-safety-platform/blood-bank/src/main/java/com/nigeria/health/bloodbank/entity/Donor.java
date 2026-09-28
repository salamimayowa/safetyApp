package com.nigeria.health.bloodbank.entity;

import com.nigeria.health.shared.enums.BloodType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Entity: Donor
 * Description: A registered blood donor's profile.
 *              Linked one-to-one with a User (DONOR role).
 *              Tracks last donation date to enforce the 56-day eligibility rule.
 *              Weight must be >= 50kg, age must be 18-65.
 * Props: none
 */
@Entity
@Table(name = "donors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BloodType bloodType;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(precision = 5, scale = 2)
    private Double weightKg;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String lga;

    private LocalDate lastDonationDate;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalDonations = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isEligible = true;

    @Column(columnDefinition = "TEXT")
    private String ineligibilityReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ─── Business logic ────────────────────────────────────────────

    /**
     * Core eligibility check — enforces the 56-day rule.
     * A donor cannot donate more than once every 56 days (8 weeks).
     */
    public boolean checkEligibility() {
        if (weightKg != null && weightKg < 50) {
            this.ineligibilityReason = "Weight must be at least 50kg";
            this.isEligible = false;
            return false;
        }
        if (getAge() < 18 || getAge() > 65) {
            this.ineligibilityReason = "Donor must be between 18 and 65 years old";
            this.isEligible = false;
            return false;
        }
        if (lastDonationDate != null) {
            long daysSinceLast = ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now());
            if (daysSinceLast < 56) {
                long daysRemaining = 56 - daysSinceLast;
                this.ineligibilityReason =
                        "Must wait " + daysRemaining + " more day(s) before donating again";
                this.isEligible = false;
                return false;
            }
        }
        this.isEligible = true;
        this.ineligibilityReason = null;
        return true;
    }

    public int getAge() {
        return (int) ChronoUnit.YEARS.between(dateOfBirth, LocalDate.now());
    }

    /** Check if today is exactly the 56th day after last donation (for reminder emails). */
    public boolean isEligibleToday() {
        if (lastDonationDate == null) return true;
        long daysSinceLast = ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now());
        return daysSinceLast >= 56;
    }
}