package com.nigeria.health.accident.entity;

import com.nigeria.health.shared.enums.AccidentSeverity;
import com.nigeria.health.shared.enums.BloodType;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity: AccidentReport
 * Description: A citizen-submitted road accident report.
 *              On creation, triggers FRSC alert and optionally blood bank notification.
 *              FATAL severity triggers immediate SUPER_ADMIN email.
 * Props: none
 *
 * Status flow: REPORTED → ACKNOWLEDGED → RESPONDERS_DISPATCHED → RESOLVED
 */
@Entity
@Table(name = "accident_reports")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccidentReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Reference code shown to reporter: ACC-2025-XXXXX */
    @Column(nullable = false, unique = true, length = 20)
    private String referenceCode;

    /** UUID of the citizen who reported — null if anonymous */
    private UUID reportedByUserId;

    /** Reporter contact for SMS updates */
    @Column(length = 20)
    private String reporterPhone;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(nullable = false, precision = 10, scale = 8)
    private Double latitude;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(nullable = false, precision = 11, scale = 8)
    private Double longitude;

    @Column(nullable = false, length = 50)
    private String state;

    @Column(nullable = false, length = 50)
    private String lga;

    @Column(columnDefinition = "TEXT")
    private String landmark;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccidentSeverity severity;

    @Column(nullable = false)
    @Builder.Default
    private Integer numberOfCasualties = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer numberOfVehicles = 1;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private BloodType bloodTypeNeeded;

    // The FRSC station that was automatically notified
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nearest_frsc_station_id")
    private FrscStation nearestFrscStation;

    // The FRSC officer assigned to handle this case
    private UUID assignedOfficerId;

    // The hospital notified about casualties
    private UUID hospitalNotifiedId;

    // Blood request auto-created if blood type needed
    private UUID bloodRequestId;

    // REPORTED, ACKNOWLEDGED, RESPONDERS_DISPATCHED, RESOLVED
    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "REPORTED";

    @OneToMany(mappedBy = "accidentReport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AccidentPhoto> photos = new ArrayList<>();

    @OneToMany(mappedBy = "accidentReport", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AccidentUpdate> updates = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime acknowledgedAt;
    private LocalDateTime resolvedAt;
}