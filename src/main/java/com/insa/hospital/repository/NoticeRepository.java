package com.insa.hospital.repository;

import com.insa.hospital.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Integer> {
    Page<Notice> findByHospitalId(String hospitalId, Pageable pageable);
    List<Notice> findByHospitalIdOrderByIdDesc(String hospitalId);
    List<Notice> findByTypeAndHospitalId(String type, String hospitalId);
}
