package com.nigeria.health.accident.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity: FrscStation
 * Description: A registered FRSC (Federal Road Safety Corps) station.
 *              When an accident is reported, the system finds the nearest station
 *              by matching state + LGA and notifies them immediately.
 * Props: none
 */
@Entity
@Table(name = "frsc_stations")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class FrscStation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

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

    @Column(length = 150)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @Column(length = 100)
    private String officerInCharge;

    @CreationTimestamp
    private LocalDateTime createdAt;
}