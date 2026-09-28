package com.nigeria.health.accident.repository;

import com.nigeria.health.accident.entity.AccidentReport;
import com.nigeria.health.shared.enums.AccidentSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccidentReportRepository extends JpaRepository<AccidentReport, UUID> {

    Optional<AccidentReport> findByReferenceCode(String referenceCode);

    Page<AccidentReport> findByState(String state, Pageable pageable);

    Page<AccidentReport> findByAssignedOfficerId(UUID officerId, Pageable pageable);

    /**
     * Find SERIOUS or FATAL reports that have not been acknowledged within 30 minutes.
     * Used by the escalation scheduler.
     */
    @Query("""
        SELECT a FROM AccidentReport a
        WHERE a.severity IN :severities
        AND a.status = 'REPORTED'
        AND a.createdAt <= :cutoffTime
        """)
    List<AccidentReport> findUnacknowledgedSeriousReports(
            @Param("severities") List<AccidentSeverity> severities,
            @Param("cutoffTime") LocalDateTime cutoffTime);

    Page<AccidentReport> findByStatus(String status, Pageable pageable);
}
