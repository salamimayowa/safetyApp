package com.nigeria.health.drug.repository;

import com.nigeria.health.drug.entity.VerificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface VerificationLogRepository extends JpaRepository<VerificationLog, UUID> {

    /**
     * Count suspicious verifications (COUNTERFEIT or NOT_FOUND) per drug per LGA
     * in the last N days. Used by hotspot detection scheduler.
     */
    @Query("""
        SELECT v.nafdacNumber, v.lga, v.state, COUNT(v) as reportCount
        FROM VerificationLog v
        WHERE v.result IN ('COUNTERFEIT','NOT_FOUND')
        AND v.createdAt >= :since
        GROUP BY v.nafdacNumber, v.lga, v.state
        HAVING COUNT(v) >= :threshold
        """)
    List<Object[]> findCounterfeitHotspots(
            @Param("since") LocalDateTime since,
            @Param("threshold") long threshold);
}
