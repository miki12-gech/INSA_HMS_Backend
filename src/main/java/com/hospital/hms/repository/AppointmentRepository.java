package com.hospital.hms.repository;

import com.hospital.hms.entity.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByDoctorOrderByIdDesc(String doctor);

    List<Appointment> findByPatientOrderByIdDesc(String patient);

    List<Appointment> findByStatusOrderByIdDesc(String status);

    List<Appointment> findByStatusAndDoctorOrderByIdDesc(String status, String doctor);

    List<Appointment> findByRequestOrderByIdDesc(String request);
    
    List<Appointment> findByRequestAndDoctorOrderByIdDesc(String request, String doctor);

    List<Appointment> findAllByOrderByIdDesc();
}
