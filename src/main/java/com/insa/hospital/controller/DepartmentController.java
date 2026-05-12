package com.insa.hospital.controller;

import com.insa.hospital.entity.Department;
import com.insa.hospital.security.JwtContextHolder;
import com.insa.hospital.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller — Department endpoints.
 *
 * Base URL: /api/departments
 * Provides CRUD for the legacy `department` table.
 */
@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @Autowired
    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /** GET /api/departments — All departments for hospital. */
    @GetMapping
    public ResponseEntity<List<Department>> listAll(
            @RequestParam(required = false) String hospitalId) {
        String effectiveHospitalId = "superadmin".equalsIgnoreCase(JwtContextHolder.getRole())
                && hospitalId != null
                && !hospitalId.trim().isEmpty()
                ? hospitalId.trim()
                : JwtContextHolder.getHospitalId();

        return ResponseEntity.ok(departmentService.listAll(effectiveHospitalId));
    }

    /** GET /api/departments/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Department> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(departmentService.getById(id, JwtContextHolder.getHospitalId()));
    }

    /** POST /api/departments/add */
    @PostMapping("/add")
    public ResponseEntity<Department> create(@RequestBody Department dept) {
        String effectiveHospitalId = "superadmin".equalsIgnoreCase(JwtContextHolder.getRole())
                ? dept.getHospitalId()
                : JwtContextHolder.getHospitalId();

        return ResponseEntity.ok(departmentService.create(dept, effectiveHospitalId));
    }

    /** PUT /api/departments/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Department> update(@PathVariable Integer id, @RequestBody Department body) {
        return ResponseEntity.ok(departmentService.update(id, body, JwtContextHolder.getHospitalId()));
    }

    /** DELETE /api/departments/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        departmentService.delete(id, JwtContextHolder.getHospitalId());
        return ResponseEntity.noContent().build();
    }
}
