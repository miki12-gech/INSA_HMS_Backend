package com.insa.hospital.service;

import com.insa.hospital.entity.Department;
import com.insa.hospital.entity.Hospital;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.DepartmentRepository;
import com.insa.hospital.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Legacy department management service for IAM administration.
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final HospitalRepository hospitalRepository;
    private final JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<Department> listAll(String hospitalId) {
        List<String> hospitalScopeIds = resolveHospitalScopeIds(hospitalId);
        List<Department> departments = !hospitalScopeIds.isEmpty()
                ? departmentRepository.findByHospitalIdIn(hospitalScopeIds)
                : departmentRepository.findAll();

        if (departments.isEmpty() && StringUtils.hasText(hospitalId)) {
            bootstrapDepartmentsFromLegacyDoctors(hospitalId.trim());
            return departmentRepository.findByHospitalIdIn(resolveHospitalScopeIds(hospitalId));
        }

        return departments;
    }

    @Transactional(readOnly = true)
    public Department getById(Integer id, String hospitalId) {
        return departmentRepository.findById(id)
                .filter(department -> belongsToHospital(department, hospitalId))
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", id));
    }

    @Transactional
    public Department create(Department request, String hospitalId) {
        String effectiveHospitalId = resolveHospitalId(request, hospitalId);

        Department department = new Department();
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        department.setHospitalId(effectiveHospitalId);
        department.setX(request.getX());
        department.setY(request.getY());

        return departmentRepository.save(department);
    }

    @Transactional
    public Department update(Integer id, Department request, String hospitalId) {
        Department department = getById(id, hospitalId);
        department.setName(request.getName());
        department.setDescription(request.getDescription());
        return departmentRepository.save(department);
    }

    @Transactional
    public void delete(Integer id, String hospitalId) {
        departmentRepository.delete(getById(id, hospitalId));
    }

    private boolean belongsToHospital(Department department, String hospitalId) {
        return !StringUtils.hasText(hospitalId)
                || resolveHospitalScopeIds(hospitalId).contains(department.getHospitalId());
    }

    private String resolveHospitalId(Department request, String hospitalId) {
        if (StringUtils.hasText(hospitalId)) {
            return normalizeHospitalScopeId(hospitalId);
        }

        if (StringUtils.hasText(request.getHospitalId())) {
            return normalizeHospitalScopeId(request.getHospitalId());
        }

        throw new IllegalArgumentException("hospitalId is required to create a department.");
    }

    private void bootstrapDepartmentsFromLegacyDoctors(String hospitalId) {
        Set<String> uniqueNames = new LinkedHashSet<>();

        for (String scopeId : resolveHospitalScopeIds(hospitalId)) {
            List<String> rawNames = jdbcTemplate.queryForList(
                    """
                    SELECT DISTINCT department
                    FROM doctor
                    WHERE hospital_id = ?
                      AND department IS NOT NULL
                      AND TRIM(department) <> ''
                    ORDER BY department ASC
                    """,
                    String.class,
                    scopeId
            );

            for (String rawName : rawNames) {
                if (StringUtils.hasText(rawName)) {
                    uniqueNames.add(rawName.trim());
                }
            }
        }

        if (uniqueNames.isEmpty()) {
            return;
        }

        List<Department> departmentsToCreate = new ArrayList<>();
        for (String name : uniqueNames) {
            Department department = new Department();
            department.setName(name);
            department.setDescription("");
            department.setHospitalId(normalizeHospitalScopeId(hospitalId));
            departmentsToCreate.add(department);
        }

        departmentRepository.saveAll(departmentsToCreate);
    }

    private List<String> resolveHospitalScopeIds(String hospitalId) {
        LinkedHashSet<String> scopeIds = new LinkedHashSet<>();
        String canonicalHospitalId = normalizeHospitalScopeId(hospitalId);

        if (StringUtils.hasText(canonicalHospitalId)) {
            scopeIds.add(canonicalHospitalId);

            hospitalRepository.findByIonUserId(canonicalHospitalId)
                    .map(Hospital::getId)
                    .map(String::valueOf)
                    .ifPresent(scopeIds::add);
        }

        if (StringUtils.hasText(hospitalId)) {
            scopeIds.add(hospitalId.trim());
        }

        return new ArrayList<>(scopeIds);
    }

    private String normalizeHospitalScopeId(String hospitalId) {
        if (!StringUtils.hasText(hospitalId)) {
            return hospitalId;
        }

        String trimmed = hospitalId.trim();
        if (hospitalRepository.findByIonUserId(trimmed).isPresent()) {
            return trimmed;
        }

        try {
            return hospitalRepository.findById(Integer.parseInt(trimmed))
                    .map(Hospital::getIonUserId)
                    .filter(StringUtils::hasText)
                    .orElse(trimmed);
        } catch (NumberFormatException ignored) {
            return trimmed;
        }
    }
}
