package com.nigeria.health.drug.repository;

import com.nigeria.health.drug.entity.CounterfeitReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CounterfeitReportRepository extends JpaRepository<CounterfeitReport, UUID> {

    long countByNafdacNumber(String nafdacNumber);

    Page<CounterfeitReport> findByStatus(String status, Pageable pageable);

    Page<CounterfeitReport> findByNafdacNumber(String nafdacNumber, Pageable pageable);
}
