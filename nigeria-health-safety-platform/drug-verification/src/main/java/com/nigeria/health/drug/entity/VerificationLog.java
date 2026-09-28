package com.nigeria.health.drug.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity: VerificationLog
 * Description: Records every drug verification query made on the platform.
 *              Stores the result and the requester's location.
 *              Used by the hotspot detection scheduler to map counterfeit activity by LGA.
 * Props: none
 */
@Entity
@Table(name = "verification_logs")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class VerificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String nafdacNumber;

    private UUID verifiedByUserId;

    /** GENUINE, COUNTERFEIT, NOT_FOUND, EXPIRED */
    @Column(nullable = false, length = 20)
    private String result;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String lga;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(precision = 10, scale = 8)
    private Double latitude;

    @JdbcTypeCode(SqlTypes.NUMERIC)
    @Column(precision = 11, scale = 8)
    private Double longitude;

    @Column(length = 200)
    private String deviceInfo;

    @CreationTimestamp
    private LocalDateTime createdAt;
}