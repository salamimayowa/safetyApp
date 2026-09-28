package com.nigeria.health.bloodbank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity: Donation
 * Description: A scheduled or completed blood donation appointment.
 *              When status = COMPLETED, the donor's last_donation_date is updated
 *              and the hospital's blood stock is increased.
 * Props: none
 *
 * Status flow: SCHEDULED → COMPLETED
 *                        → CANCELLED (donor cancels)
 *                        → NO_SHOW (donor doesn't appear)
 */
@Entity
@Table(name = "donations")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "donor_id", nullable = false)
    private Donor donor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    private LocalDateTime appointmentDate;

    private LocalDateTime donationDate; // set when COMPLETED

    @Column(precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal unitsDonated = BigDecimal.ONE; // typically 1 unit (450ml)

    // SCHEDULED, COMPLETED, CANCELLED, NO_SHOW
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "SCHEDULED";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
