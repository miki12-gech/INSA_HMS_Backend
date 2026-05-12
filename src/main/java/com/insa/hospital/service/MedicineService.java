package com.insa.hospital.service;

import com.insa.hospital.dto.MedicineRequestDto;
import com.insa.hospital.dto.MedicineResponseDto;
import com.insa.hospital.entity.Medicine;
import com.insa.hospital.exception.ResourceNotFoundException;
import com.insa.hospital.repository.MedicineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Transactional
public class MedicineService {

    private static final DateTimeFormatter ADD_DATE_FMT =
            DateTimeFormatter.ofPattern("MM/dd/yy");

    private final MedicineRepository medicineRepository;

    @Autowired
    public MedicineService(MedicineRepository medicineRepository) {
        this.medicineRepository = medicineRepository;
    }

    // ─── Create ───────────────────────────────────────────────────────────────

    public MedicineResponseDto createMedicine(MedicineRequestDto dto, String hospitalId, Integer userId) {
        Medicine m = new Medicine();
        mapDtoToEntity(dto, m);
        m.setHospitalId(hospitalId);
        m.setUser(userId);
        m.setAddDate(LocalDate.now().format(ADD_DATE_FMT));
        return MedicineResponseDto.from(medicineRepository.save(m));
    }

    // ─── List / Search ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<MedicineResponseDto> listMedicines(String hospitalId, Pageable pageable) {
        return medicineRepository.findByHospitalId(hospitalId, pageable)
                .map(MedicineResponseDto::from);
    }

    /** Unpaged — for prescription dropdown. */
    @Transactional(readOnly = true)
    public List<MedicineResponseDto> listAllMedicines(String hospitalId) {
        return medicineRepository.findByHospitalId(hospitalId)
                .stream().map(MedicineResponseDto::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<MedicineResponseDto> searchMedicines(String hospitalId, String query, Pageable pageable) {
        return medicineRepository.searchByNameOrGeneric(hospitalId, query, pageable)
                .map(MedicineResponseDto::from);
    }

    @Transactional(readOnly = true)
    public MedicineResponseDto getById(Integer id, String hospitalId) {
        Medicine m = medicineRepository.findById(id)
                .filter(med -> hospitalId.equals(med.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", "id", id));
        return MedicineResponseDto.from(m);
    }

    // ─── Stock & Values ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<MedicineResponseDto> listStockAlerts(String hospitalId, Pageable pageable) {
        return medicineRepository.findByHospitalIdAndQuantityLessThanEqual(hospitalId, 20, pageable)
                .map(MedicineResponseDto::from);
    }

    @Transactional(readOnly = true)
    public Double getTotalStockPrice(String hospitalId) {
        List<Medicine> all = medicineRepository.findByHospitalId(hospitalId);
        return all.stream()
                .filter(m -> m.getPrice() != null && m.getQuantity() != null)
                .mapToDouble(m -> {
                    try {
                        return Double.parseDouble(m.getPrice()) * m.getQuantity();
                    } catch (NumberFormatException e) {
                        return 0.0;
                    }
                })
                .sum();
    }

    // ─── Update ───────────────────────────────────────────────────────────────

    public MedicineResponseDto updateMedicine(Integer id, MedicineRequestDto dto, String hospitalId) {
        Medicine m = medicineRepository.findById(id)
                .filter(med -> hospitalId.equals(med.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", "id", id));
        mapDtoToEntity(dto, m);
        return MedicineResponseDto.from(medicineRepository.save(m));
    }

    public void loadMedicine(Integer id, Integer qtyToAdd, String hospitalId) {
        Medicine m = medicineRepository.findById(id)
                .filter(med -> hospitalId.equals(med.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", "id", id));
        int current = m.getQuantity() == null ? 0 : m.getQuantity();
        m.setQuantity(current + qtyToAdd);
        medicineRepository.save(m);
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    public void deleteMedicine(Integer id, String hospitalId) {
        Medicine m = medicineRepository.findById(id)
                .filter(med -> hospitalId.equals(med.getHospitalId()))
                .orElseThrow(() -> new ResourceNotFoundException("Medicine", "id", id));
        medicineRepository.delete(m);
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private void mapDtoToEntity(MedicineRequestDto dto, Medicine m) {
        m.setName(dto.name());
        m.setCategory(dto.category());
        m.setCategory1(dto.category1());
        m.setPrice(dto.price());
        m.setSPrice(dto.sPrice());
        m.setBox(dto.box());
        m.setQuantity(dto.quantity());
        m.setGeneric(dto.generic());
        m.setCompany(dto.company());
        m.setEffects(dto.effects());
        m.setEDate(dto.eDate());
        m.setStrength(dto.strength());
        
        // Exact legacy PHP e_date_n generation (Unix Epoch string based on DD-MM-YYYY / MM-DD-YYYY or empty string fallback)
        if (dto.eDate() != null && !dto.eDate().isBlank()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                // Allow parsing if frontend passes standard ISO
                Date parsedDate = sdf.parse(dto.eDate());
                m.setEDateN(String.valueOf(parsedDate.getTime() / 1000L));
            } catch (Exception e1) {
                try {
                    SimpleDateFormat sdf2 = new SimpleDateFormat("dd-MM-yyyy");
                    Date parsedDate = sdf2.parse(dto.eDate());
                    m.setEDateN(String.valueOf(parsedDate.getTime() / 1000L));
                } catch (Exception e2) {
                    m.setEDateN("");
                }
            }
        } else {
            m.setEDateN("");
        }
    }
}
