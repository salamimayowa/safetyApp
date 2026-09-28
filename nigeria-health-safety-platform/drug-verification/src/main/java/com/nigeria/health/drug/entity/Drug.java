package com.nigeria.health.drug.entity;

import com.nigeria.health.shared.enums.DrugCategory;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity: Drug
 * Description: A drug registered in the NAFDAC verification database.
 *              Must be submitted by PHARMACY_ADMIN and approved by SUPER_ADMIN
 *              before it appears in verification results.
 * Props: none
 */
@Entity
@Table(name = "drugs")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Drug {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String brandName;

    @Column(nullable = false, length = 150)
    private String genericName;

    /** NAFDAC registration number — unique identifier used for verification */
    @Column(nullable = false, unique = true, length = 50)
    private String nafdacNumber;

    @Column(nullable = false, length = 150)
    private String manufacturer;

    @Column(length = 100)
    private String countryOfOrigin;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private DrugCategory category;

    private LocalDate registrationDate;

    private LocalDate expiryDate;

    /** Tablet, Capsule, Syrup, Injection, Cream */
    @Column(length = 50)
    private String dosageForm;

    @Column(columnDefinition = "TEXT")
    private String storageInstructions;

    /** Must be true before drug appears in verification results */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isApproved = false;

    private UUID addedBy;
    private UUID approvedBy;
    private LocalDateTime approvedAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // ─── Business logic ────────────────────────────────────────────

    public boolean isExpired() {
        return expiryDate != null && LocalDate.now().isAfter(expiryDate);
    }
}
