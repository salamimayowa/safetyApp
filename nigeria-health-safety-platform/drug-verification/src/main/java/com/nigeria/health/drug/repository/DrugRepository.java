package com.nigeria.health.drug.repository;

import com.nigeria.health.drug.entity.Drug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DrugRepository extends JpaRepository<Drug, UUID> {

    Optional<Drug> findByNafdacNumber(String nafdacNumber);

    boolean existsByNafdacNumber(String nafdacNumber);

    Page<Drug> findByIsApprovedTrue(Pageable pageable);

    Page<Drug> findByIsApprovedFalse(Pageable pageable);

    Page<Drug> findByAddedBy(UUID addedBy, Pageable pageable);

    Page<Drug> findByBrandNameContainingIgnoreCaseOrGenericNameContainingIgnoreCase(
            String brand, String generic, Pageable pageable);
}
