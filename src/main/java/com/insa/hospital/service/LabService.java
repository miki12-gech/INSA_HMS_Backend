package com.insa.hospital.service;

import com.insa.hospital.dto.*;
import com.insa.hospital.entity.*;
import com.insa.hospital.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LabService {

    private final LabRepository labRepository;
    private final LabCategoryRepository labCategoryRepository;
    private final TemplateRepository templateRepository;
    private final LabTemplateDetailsRepository labTemplateDetailsRepository;
    
    // Assuming these repositories exist
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final TenantAccessService tenantAccessService;

    @Transactional
    public void saveLab(LabRequestDto request) {
        String effectiveHospitalId = tenantAccessService.getCurrentHospitalId();
        String patientId = request.patient();
        String doctorId = request.doctor();

        // Handle Add New Patient inline if passed
        if ("add_new".equals(patientId) && request.p_name() != null) {
            Patient newPatient = new Patient();
            newPatient.setName(request.p_name());
            newPatient.setEmail(request.p_email());
            newPatient.setPhone(request.p_phone());
            newPatient.setSex(request.p_gender());
            newPatient.setAge(request.p_age());
            newPatient.setHospitalId(effectiveHospitalId);
            newPatient.setAddDate(new SimpleDateFormat("MM/dd/yy").format(new Date()));
            Patient savedPatient = patientRepository.save(newPatient);
            patientId = savedPatient.getId().toString();
        }

        // Handle Add New Doctor inline if passed
        if ("add_new".equals(doctorId) && request.d_name() != null) {
            Doctor newDoctor = new Doctor();
            newDoctor.setName(request.d_name());
            newDoctor.setEmail(request.d_email());
            newDoctor.setPhone(request.d_phone());
            newDoctor.setHospitalId(effectiveHospitalId);
            Doctor savedDoctor = doctorRepository.save(newDoctor);
            doctorId = savedDoctor.getId().toString();
        }

        // Fetch Metadata for nesting (Legacy logic)
        String pName = "0";
        String pPhone = "0";
        String pAddress = "0";
        String dName = "0";

        if (patientId != null && !patientId.isEmpty() && !patientId.equals("add_new")) {
            Optional<Patient> popt = patientRepository.findById(Integer.parseInt(patientId));
            if (popt.isPresent()) {
                Patient p = popt.get();
                pName = p.getName() != null ? p.getName() : "0";
                pPhone = p.getPhone() != null ? p.getPhone() : "0";
                pAddress = p.getAddress() != null ? p.getAddress() : "0";
            }
        }

        if (doctorId != null && !doctorId.isEmpty() && !doctorId.equals("add_new")) {
            Optional<Doctor> dopt = doctorRepository.findById(Integer.parseInt(doctorId));
            if (dopt.isPresent()) {
                Doctor d = dopt.get();
                dName = d.getName() != null ? d.getName() : "0";
            }
        }

        // Create the Lab entity
        Lab lab = new Lab();
        // If updating an existing Lab (using same method for logic simplicity)
        if (request.id() != null) {
            lab = labRepository.findById(request.id())
                    .orElseThrow(() -> new RuntimeException("Lab not found"));
        }

        lab.setReport(request.report());
        lab.setPatient(patientId);
        
        long dateMillis = System.currentTimeMillis();
        if (request.date() != null && !request.date().isEmpty()) {
            try {
                // If it's passed as seconds or formatted date, we parse it, here we assume direct string or standard format
                // Legacy system used strtotime(). Here we just save the string.
                lab.setDate(request.date());
            } catch (Exception e) {
                lab.setDate(String.valueOf(dateMillis));
            }
        } else {
            lab.setDate(String.valueOf(dateMillis));
        }

        lab.setDoctor(doctorId);
        lab.setStatus(request.status());
        lab.setPatientName(pName);
        lab.setPatientPhone(pPhone);
        lab.setPatientAddress(pAddress);
        lab.setDoctorName(dName);
        lab.setDateString(new SimpleDateFormat("dd-MM-yy").format(new Date(dateMillis)));
        lab.setHospitalId(effectiveHospitalId);

        Lab savedLab = labRepository.save(lab);

        // Process templates
        if (request.templet_id() != null && !request.templet_id().trim().isEmpty()) {
            
            // If this is an update, delete old templates first (legacy logic typically wiped or just appended pending implementation, we'll wipe and recreate)
            if (request.id() != null) {
                labTemplateDetailsRepository.deleteByLabId(savedLab.getId().toString());
            }

            String[] templateIds = request.templet_id().split("\\*");
            for (String tid : templateIds) {
                if (tid != null && !tid.isEmpty()) {
                    LabTemplateDetails details = new LabTemplateDetails();
                    details.setLabId(savedLab.getId().toString());
                    details.setTemplateId(tid.trim());
                    labTemplateDetailsRepository.save(details);
                }
            }
        }
    }

    // Generic list fetches with DTO mappings
    public List<LabResponseDto> getAllLabs() {
        List<Lab> rows = tenantAccessService.isSuperAdmin()
                ? labRepository.findAll().stream()
                    .sorted(java.util.Comparator.comparing(Lab::getId).reversed())
                    .toList()
                : labRepository.findVisibleByHospitalIdsOrderByIdDesc(
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser()
                );
        return rows.stream().map(this::mapToLabDto).collect(Collectors.toList());
    }

    public LabResponseDto getLabById(Long id) {
        return labRepository.findById(id)
                .filter(lab -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(lab.getHospitalId()))
                .map(this::mapToLabDto)
                .orElse(null);
    }
    
    @Transactional
    public void deleteLab(Long id) {
        labRepository.findById(id)
                .filter(lab -> tenantAccessService.isSuperAdmin()
                        || tenantAccessService.belongsToCurrentScope(lab.getHospitalId()))
                .ifPresent(labRepository::delete);
        labTemplateDetailsRepository.deleteByLabId(id.toString());
    }

    private LabResponseDto mapToLabDto(Lab lab) {
        List<String> tIds = labTemplateDetailsRepository.findByLabId(lab.getId().toString())
            .stream()
            .map(LabTemplateDetails::getTemplateId)
            .collect(Collectors.toList());

        return new LabResponseDto(
                lab.getId(),
                lab.getReport(),
                lab.getPatient(),
                lab.getDate(),
                lab.getDoctor(),
                lab.getStatus(),
                lab.getUser(),
                lab.getPatientName(),
                lab.getPatientPhone(),
                lab.getPatientAddress(),
                lab.getDoctorName(),
                lab.getDateString(),
                tIds
        );
    }

    // --- Lab Category ---
    public List<LabCategoryDto> getCategories() {
        return labCategoryRepository.findAll().stream().map(c -> new LabCategoryDto(
                c.getId(), c.getCategory(), c.getDescription(), c.getReferenceValue()
        )).collect(Collectors.toList());
    }

    public void saveCategory(LabCategoryDto dto) {
        LabCategory c = new LabCategory();
        if(dto.id() != null) {
            c = labCategoryRepository.findById(dto.id()).orElse(new LabCategory());
        }
        c.setCategory(dto.category());
        c.setDescription(dto.description());
        c.setReferenceValue(dto.reference_value());
        labCategoryRepository.save(c);
    }

    public void deleteCategory(Long id) {
        labCategoryRepository.deleteById(id);
    }

    // --- Templates ---
    public List<TemplateDto> getTemplates() {
        return templateRepository.findAll().stream().map(t -> new TemplateDto(
                t.getId(), t.getName(), t.getTemplate(), t.getCategory(), t.getUser()
        )).collect(Collectors.toList());
    }

    public void saveTemplate(TemplateDto dto) {
        Template t = new Template();
        if(dto.id() != null) {
            t = templateRepository.findById(dto.id()).orElse(new Template());
        }
        t.setName(dto.name());
        t.setTemplate(dto.template());
        t.setCategory(dto.category());
        t.setUser(dto.user());
        templateRepository.save(t);
    }

    public void deleteTemplate(Long id) {
        templateRepository.deleteById(id);
    }
}
