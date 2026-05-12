package com.insa.hospital.repository;

import com.insa.hospital.entity.BloodBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BloodBankRepository extends JpaRepository<BloodBank, Integer> {

    List<BloodBank> findByHospitalId(String hospitalId);

    Optional<BloodBank> findByGroupAndHospitalId(String group, String hospitalId);
}
