package com.nigeria.health.drug.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity: CounterfeitReport
 * Description: A citizen or pharmacist's report of a suspected counterfeit drug.
 *              When 5+ reports are filed against the same NAFDAC number,
 *              the system auto-escalates to SUPER_ADMIN.
 * Props: none
 *
 * Status flow: PENDING → INVESTIGATING → CONFIRMED | DISMISSED
 */
@Entity
@Table(name = "counterfeit_reports")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class CounterfeitReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 50)
    private String nafdacNumber;

    private UUID drugId;

    private UUID reportedByUserId;

    @Column(length = 150)
    private String pharmacyName;

    @Column(columnDefinition = "TEXT")
    private String sellerAddress;

    @Column(length = 50)
    private String state;

    @Column(length = 50)
    private String lga;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String evidencePhotoUrl;

    /** PENDING, INVESTIGATING, CONFIRMED, DISMISSED */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    private UUID investigatedBy;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;
}
