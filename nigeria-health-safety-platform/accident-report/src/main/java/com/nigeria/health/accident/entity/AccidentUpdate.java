package com.nigeria.health.accident.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "accident_updates")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AccidentUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accident_report_id", nullable = false)
    private AccidentReport accidentReport;

    private UUID updatedByUserId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(length = 30)
    private String statusChangedTo;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
