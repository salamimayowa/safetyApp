package com.nigeria.health.bloodbank.entity;

import com.nigeria.health.shared.enums.BloodType;
import com.nigeria.health.shared.enums.UrgencyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity: BloodRequest
 * Description: A hospital's request for blood units from the network.
 *              CRITICAL requests trigger immediate broadcast to all nearby hospitals.
 *              Can be linked to an accident report (accidentReportId).
 *              Auto-expires after 24 hours if not fulfilled.
 * Props: none
 */
@Entity
@Table(name = "blood_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BloodRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requesting_hospital_id", nullable = false)
    private Hospital requestingHospital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BloodType bloodType;

    @Column(nullable = false)
    private Integer unitsNeeded;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UrgencyLevel urgency;

    @Column(length = 100)
    private String patientName;

    @Column(columnDefinition = "TEXT")
    private String reason;

    // OPEN, PARTIALLY_FULFILLED, FULFILLED, CANCELLED, EXPIRED
    @Column(nullable = false, length = 25)
    @Builder.Default
    private String status = "OPEN";

    // The hospital that fulfilled this request
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fulfilled_by_hospital_id")
    private Hospital fulfilledByHospital;

    // Links to accident module when auto-created from an accident report
    private UUID accidentReportId;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime fulfilledAt;

    // Auto-set to now + 24hrs on creation
    private LocalDateTime expiresAt;

    // ─── Business logic ────────────────────────────────────────────

    public boolean isOpen() {
        return "OPEN".equals(status);
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isCritical() {
        return UrgencyLevel.CRITICAL.equals(urgency);
    }
}
