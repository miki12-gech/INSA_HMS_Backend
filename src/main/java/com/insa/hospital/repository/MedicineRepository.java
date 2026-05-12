package com.insa.hospital.repository;

import com.insa.hospital.entity.Medicine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicineRepository extends JpaRepository<Medicine, Integer> {

    Page<Medicine> findByHospitalId(String hospitalId, Pageable pageable);

    /** Unpaged list for prescription dropdowns. */
    List<Medicine> findByHospitalId(String hospitalId);

    /** Search by name or generic name. */
    @Query("""
        SELECT m FROM Medicine m
        WHERE m.hospitalId = :hospitalId
        AND (LOWER(m.name) LIKE LOWER(CONCAT('%', :query, '%'))
             OR LOWER(m.generic) LIKE LOWER(CONCAT('%', :query, '%')))
        """)
    Page<Medicine> searchByNameOrGeneric(
            @Param("hospitalId") String hospitalId,
            @Param("query") String query,
            Pageable pageable);

    Page<Medicine> findByCategoryAndHospitalId(String category, String hospitalId, Pageable pageable);

    List<Medicine> findByQuantityGreaterThan(Integer quantity);

    @Query("""
        SELECT m FROM Medicine m
        WHERE m.quantity > :minimumQuantity
          AND (m.hospitalId IN :hospitalIds
               OR (:includeBlank = true AND COALESCE(TRIM(m.hospitalId), '') = ''))
        ORDER BY m.id DESC
        """)
    List<Medicine> findVisibleByHospitalIdsAndQuantityGreaterThan(
            @Param("hospitalIds") List<String> hospitalIds,
            @Param("includeBlank") boolean includeBlank,
            @Param("minimumQuantity") Integer minimumQuantity);

    Page<Medicine> findByHospitalIdAndQuantityLessThanEqual(String hospitalId, Integer quantity, Pageable pageable);
}
