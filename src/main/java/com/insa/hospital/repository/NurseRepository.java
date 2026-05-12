package com.insa.hospital.repository;

import com.insa.hospital.entity.Nurse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NurseRepository extends JpaRepository<Nurse, Integer> {
    List<Nurse> findByHospitalId(String hospitalId);
    Optional<Nurse> findByIonUserIdAndHospitalId(String ionUserId, String hospitalId);
}
