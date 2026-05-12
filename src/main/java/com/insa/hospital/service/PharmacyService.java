package com.insa.hospital.service;

import com.insa.hospital.dto.MedicineDto;
import com.insa.hospital.dto.MedicineIssueRequestDto;
import com.insa.hospital.dto.PharmacyPosRequestDto;
import com.insa.hospital.dto.SmartDispenseRequestDto;
import com.insa.hospital.entity.Medicine;
import com.insa.hospital.entity.PharmacyPayment;
import com.insa.hospital.entity.MedicineIssue;
import com.insa.hospital.entity.MedicineIssueDet;
import com.insa.hospital.entity.Prescription;
import com.insa.hospital.entity.Patient;
import com.insa.hospital.exception.InsufficientStockException;
import com.insa.hospital.repository.MedicineRepository;
import com.insa.hospital.repository.PharmacyPaymentRepository;
import com.insa.hospital.repository.MedicineIssueRepository;
import com.insa.hospital.repository.MedicineIssueDetRepository;
import com.insa.hospital.repository.PrescriptionRepository;
import com.insa.hospital.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PharmacyService {

    @Autowired
    private TenantAccessService tenantAccessService;

    @Autowired
    private MedicineRepository medicineRepository;

    @Autowired
    private PharmacyPaymentRepository pharmacyPaymentRepository;

    @Autowired
    private MedicineIssueRepository medicineIssueRepository;

    @Autowired
    private MedicineIssueDetRepository medicineIssueDetRepository;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PatientRepository patientRepository;

    public List<MedicineDto> getAllMedicines() {
        List<Medicine> medicines = tenantAccessService.isSuperAdmin()
                ? medicineRepository.findByQuantityGreaterThan(0)
                : medicineRepository.findVisibleByHospitalIdsAndQuantityGreaterThan(
                        tenantAccessService.getCurrentHospitalScopeIds(),
                        tenantAccessService.includeBlankHospitalRowsForCurrentUser(),
                        0
                );

        return medicines.stream().map(medicine -> {
            MedicineDto dto = new MedicineDto();
            dto.setId(medicine.getId());
            dto.setName(medicine.getName());
            dto.setCategory(medicine.getCategory());
            dto.setPrice(medicine.getPrice());
            dto.setQuantity(medicine.getQuantity());
            dto.setSPrice(medicine.getSPrice());
            dto.setGeneric(medicine.getGeneric());
            dto.setCompany(medicine.getCompany());
            dto.setEDate(medicine.getEDate());
            dto.setHospitalId(medicine.getHospitalId());
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public PharmacyPayment processSmartDispense(SmartDispenseRequestDto dto) {
        String effectiveHospitalId = tenantAccessService.getCurrentHospitalId();
        double totalGrossAmount = 0.0;
        StringBuilder categoryNameBuilder = new StringBuilder();

        for (SmartDispenseRequestDto.MedicineDispenseItem item : dto.getMedicinesToDispense()) {
            Medicine medicine = medicineRepository.findById(item.getMedicineId())
                    .orElseThrow(() -> new RuntimeException("Medicine not found with ID: " + item.getMedicineId()));
            if (!tenantAccessService.isSuperAdmin()
                    && !tenantAccessService.belongsToCurrentScope(medicine.getHospitalId())) {
                throw new RuntimeException("Medicine is outside the current hospital scope.");
            }

            if (medicine.getQuantity() == null || medicine.getQuantity() < item.getQuantitySold()) {
                throw new InsufficientStockException("Not enough quantity in stock for: " + medicine.getName());
            }

            // Deduct stock
            medicine.setQuantity(medicine.getQuantity() - item.getQuantitySold());
            medicineRepository.save(medicine);

            double price = parseDoubleSafe(medicine.getSPrice(), medicine.getPrice());
            double itemTotal = price * item.getQuantitySold();
            totalGrossAmount += itemTotal;

            if (categoryNameBuilder.length() > 0) {
                categoryNameBuilder.append(",");
            }
            categoryNameBuilder.append(medicine.getId()).append("*")
                    .append(price).append("*")
                    .append(item.getQuantitySold()).append("*")
                    .append(medicine.getPrice() != null ? medicine.getPrice() : "0");
        }

        PharmacyPayment payment = new PharmacyPayment();
        payment.setPatient(dto.getPatientId());
        payment.setAmount(String.valueOf(totalGrossAmount));
        payment.setGrossTotal(String.valueOf(totalGrossAmount));
        payment.setDate(String.valueOf(System.currentTimeMillis() / 1000));
        payment.setCategoryName(categoryNameBuilder.toString());
        payment.setStatus("unpaid");
        payment.setDiscount("0");
        payment.setVat("0");
        payment.setHospitalId(effectiveHospitalId);
        payment.setCategory("prescription");

        PharmacyPayment saved = pharmacyPaymentRepository.save(payment);

        Prescription prescription = prescriptionRepository.findById(dto.getPrescriptionId())
                .orElseThrow(() -> new RuntimeException("Prescription not found with ID: " + dto.getPrescriptionId()));
        if (!tenantAccessService.isSuperAdmin()
                && !tenantAccessService.belongsToCurrentScope(prescription.getHospitalId())) {
            throw new RuntimeException("Prescription is outside the current hospital scope.");
        }
        prescription.setState("DISPENSED");
        prescriptionRepository.save(prescription);

        return saved;
    }

    @Transactional
    public PharmacyPayment addPayment(PharmacyPosRequestDto dto) {
        String effectiveHospitalId = tenantAccessService.getCurrentHospitalId();
        double subTotal = 0.0;
        StringBuilder categoryNameBuilder = new StringBuilder();

        for (PharmacyPosRequestDto.PosItemDto item : dto.getItems()) {
            Medicine medicine = medicineRepository.findById(Integer.valueOf(item.getMedicineId()))
                    .orElseThrow(() -> new RuntimeException("Medicine not found"));
            if (!tenantAccessService.isSuperAdmin()
                    && !tenantAccessService.belongsToCurrentScope(medicine.getHospitalId())) {
                throw new RuntimeException("Medicine is outside the current hospital scope.");
            }

            if (medicine.getQuantity() == null || medicine.getQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient Quantity selected for Medicine " + medicine.getName());
            }

            double unitPrice = parseDoubleSafe(medicine.getSPrice(), "0");
            double cost = parseDoubleSafe(medicine.getPrice(), "0");
            double itemPrice = unitPrice * item.getQuantity();
            subTotal += itemPrice;

            if (categoryNameBuilder.length() > 0) {
                categoryNameBuilder.append(",");
            }
            // `id*unit_price*qty*cost` matching exactly legacy string logic
            categoryNameBuilder.append(medicine.getId()).append("*")
                    .append(unitPrice).append("*")
                    .append(item.getQuantity()).append("*")
                    .append(cost);
            
            // Deduct from stock
            medicine.setQuantity(medicine.getQuantity() - item.getQuantity());
            medicineRepository.save(medicine);
            
            // Legacy code also deducted from `medicine_issue_det` total quantity but only for specific logic.
            // We replicate exactly updating `medicine_issue_det` quantities sequentially until valueIssue <= 0
            int valueIssue = item.getQuantity();
            List<MedicineIssueDet> issueDets = medicineIssueDetRepository.findByMedicineId(String.valueOf(medicine.getId()));
            for (MedicineIssueDet det : issueDets) {
                if (valueIssue > 0) {
                    int prevQty = det.getQuantity() != null ? det.getQuantity() : 0;
                    int resQty = prevQty - valueIssue;
                    if (resQty < 0) {
                        valueIssue = resQty * -1;
                        det.setQuantity(0);
                    } else {
                        det.setQuantity(resQty);
                        valueIssue = 0;
                    }
                    medicineIssueDetRepository.save(det);
                }
            }
        }

        double discountVal = parseDoubleSafe(dto.getDiscount(), "0");
        double flatDiscount = subTotal * (discountVal / 100); // Or flat discount logic depending on discount_type
        double grossTotal = subTotal - flatDiscount;

        String finalPatientId = dto.getPatient();
        if ((finalPatientId == null || finalPatientId.trim().isEmpty()) && dto.getGuestName() != null && !dto.getGuestName().trim().isEmpty()) {
            Patient newGuest = new Patient();
            newGuest.setName(dto.getGuestName());
            newGuest.setPhone(dto.getGuestPhone());
            newGuest.setHowAdded("from_pos");
            newGuest.setHospitalId(effectiveHospitalId);
            newGuest.setAddDate(new SimpleDateFormat("MM/dd/yy").format(new Date()));
            newGuest.setRegistrationTime(String.valueOf(System.currentTimeMillis() / 1000));
            Patient savedGuest = patientRepository.save(newGuest);
            finalPatientId = String.valueOf(savedGuest.getId());
        }

        PharmacyPayment p = new PharmacyPayment();
        p.setCategoryName(categoryNameBuilder.toString());
        p.setPatient(finalPatientId);
        p.setDoctor(dto.getDoctor());
        p.setXRay(dto.getXRay());
        p.setDate(dto.getDate() != null ? dto.getDate() : String.valueOf(System.currentTimeMillis() / 1000));
        p.setAmount(String.valueOf(subTotal));
        p.setDiscount(String.valueOf(discountVal));
        p.setFlatDiscount(String.valueOf(flatDiscount));
        p.setGrossTotal(String.valueOf(grossTotal));
        p.setAmountReceived(dto.getAmountReceived());
        p.setStatus("unpaid");
        p.setVat("0");
        p.setHospitalId(effectiveHospitalId);
        p.setCategory("direct");

        return pharmacyPaymentRepository.save(p);
    }

    @Transactional
    public MedicineIssue addMedicineIssue(MedicineIssueRequestDto dto) {
        String effectiveHospitalId = tenantAccessService.getCurrentHospitalId();
        StringBuilder categoryNameBuilder = new StringBuilder();
        double totalCost = 0.0;

        for (MedicineIssueRequestDto.MedicineIssueItemDto item : dto.getItems()) {
            Medicine medicine = medicineRepository.findById(Integer.valueOf(item.getMedicineId()))
                    .orElseThrow(() -> new RuntimeException("Medicine not found"));
            if (!tenantAccessService.isSuperAdmin()
                    && !tenantAccessService.belongsToCurrentScope(medicine.getHospitalId())) {
                throw new RuntimeException("Medicine is outside the current hospital scope.");
            }

            if (medicine.getQuantity() == null || medicine.getQuantity() < item.getQuantity()) {
                throw new InsufficientStockException("Insufficient Quantity selected for Medicine " + medicine.getName());
            }

            double unitPrice = parseDoubleSafe(medicine.getSPrice(), "0");
            double cost = parseDoubleSafe(medicine.getPrice(), "0");

            if (categoryNameBuilder.length() > 0) {
                categoryNameBuilder.append(",");
            }
            categoryNameBuilder.append(medicine.getId()).append("*")
                    .append(unitPrice).append("*")
                    .append(item.getQuantity()).append("*")
                    .append(cost);
        }

        long timestamp = dto.getDate() != null ? Long.parseLong(dto.getDate()) : System.currentTimeMillis() / 1000;
        Date date = new Date(timestamp * 1000);
        SimpleDateFormat dateStringFmt = new SimpleDateFormat("dd-MM-yy");
        SimpleDateFormat dateTimeStringFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        MedicineIssue issue = new MedicineIssue();
        issue.setMedicineId(categoryNameBuilder.toString());
        issue.setCategoryId(dto.getCategoryId());
        issue.setReceiverName(dto.getReceiverName());
        issue.setUser(dto.getUser());
        issue.setDate(String.valueOf(timestamp));
        issue.setDateString(dateStringFmt.format(date));
        issue.setDatetimeString(dateTimeStringFmt.format(date));
        issue.setStatus("0");
        issue.setHospitalId(effectiveHospitalId);

        MedicineIssue saved = medicineIssueRepository.save(issue);

        for (MedicineIssueRequestDto.MedicineIssueItemDto item : dto.getItems()) {
            MedicineIssueDet det = new MedicineIssueDet();
            det.setMedicineId(String.valueOf(item.getMedicineId()));
            det.setQuantity(item.getQuantity());
            det.setReceiverQuantity(item.getQuantity());
            det.setRemark(dto.getUser());
            det.setMedicineIssueId(saved.getId().intValue());
            medicineIssueDetRepository.save(det);
        }
        return saved;
    }

    private double parseDoubleSafe(String val1, String val2) {
        try {
            if (val1 != null && !val1.isEmpty()) {
                return Double.parseDouble(val1);
            }
            if (val2 != null && !val2.isEmpty()) {
                return Double.parseDouble(val2);
            }
        } catch (NumberFormatException e) {
            // suppress
        }
        return 0.0;
    }
}

