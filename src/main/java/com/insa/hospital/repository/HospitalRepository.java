package com.insa.hospital.repository;

import com.insa.hospital.entity.Hospital;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Integer> {
    Optional<Hospital> findByIonUserId(String ionUserId);
    Optional<Hospital> findByNameIgnoreCase(String name);
    Optional<Hospital> findFirstByOrderByIdAsc();
}
