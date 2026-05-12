package com.hospital.hms.service;

import com.hospital.hms.dto.OtPaymentDto;
import com.hospital.hms.dto.PatientDepositDto;
import com.hospital.hms.dto.PaymentCategoryDto;
import com.hospital.hms.dto.PaymentDto;
import com.hospital.hms.entity.OtPayment;
import com.hospital.hms.entity.PatientDeposit;
import com.hospital.hms.entity.Payment;
import com.hospital.hms.entity.PaymentCategory;
import com.hospital.hms.repository.OtPaymentRepository;
import com.hospital.hms.repository.PatientDepositRepository;
import com.hospital.hms.repository.PaymentCategoryRepository;
import com.hospital.hms.repository.PaymentRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OtPaymentRepository otPaymentRepository;
    private final PatientDepositRepository patientDepositRepository;
    private final PaymentCategoryRepository paymentCategoryRepository;

    public PaymentService(PaymentRepository paymentRepository,
                          OtPaymentRepository otPaymentRepository,
                          PatientDepositRepository patientDepositRepository,
                          PaymentCategoryRepository paymentCategoryRepository) {
        this.paymentRepository = paymentRepository;
        this.otPaymentRepository = otPaymentRepository;
        this.patientDepositRepository = patientDepositRepository;
        this.paymentCategoryRepository = paymentCategoryRepository;
    }

    // --- Payment Methods ---
    public PaymentDto createPayment(PaymentDto dto) {
        Payment entity = new Payment();
        BeanUtils.copyProperties(dto, entity);
        // Legacy calculation parity: if discount and flatDiscount are provided, calculate gross
        calculateGrossTotal(entity);
        Payment saved = paymentRepository.save(entity);
        dto.setId(saved.getId());
        return dto;
    }

    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(this::convertToPaymentDto)
                .collect(Collectors.toList());
    }

    private void calculateGrossTotal(Payment payment) {
        if (payment.getAmount() != null) {
            try {
                double amount = Double.parseDouble(payment.getAmount());
                double flatDiscount = payment.getFlatDiscount() != null && !payment.getFlatDiscount().isEmpty() 
                                      ? Double.parseDouble(payment.getFlatDiscount()) : 0.0;
                double vat = payment.getFlatVat() != null && !payment.getFlatVat().isEmpty() 
                             ? Double.parseDouble(payment.getFlatVat()) : 0.0;
                
                double gross = amount - flatDiscount + vat;
                payment.setGrossTotal(String.valueOf(gross));
            } catch (NumberFormatException e) {
                // Ignore parsing errors, keep original value
            }
        }
    }

    private PaymentDto convertToPaymentDto(Payment entity) {
        PaymentDto dto = new PaymentDto();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    // --- OT Payment Methods ---
    public OtPaymentDto createOtPayment(OtPaymentDto dto) {
        OtPayment entity = new OtPayment();
        BeanUtils.copyProperties(dto, entity);
        OtPayment saved = otPaymentRepository.save(entity);
        dto.setId(saved.getId());
        return dto;
    }

    public List<OtPaymentDto> getAllOtPayments() {
        return otPaymentRepository.findAll().stream().map(entity -> {
            OtPaymentDto dto = new OtPaymentDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    // --- Patient Deposit Methods ---
    public PatientDepositDto createPatientDeposit(PatientDepositDto dto) {
        PatientDeposit entity = new PatientDeposit();
        BeanUtils.copyProperties(dto, entity);
        PatientDeposit saved = patientDepositRepository.save(entity);
        dto.setId(saved.getId());
        return dto;
    }

    public List<PatientDepositDto> getAllPatientDeposits() {
        return patientDepositRepository.findAll().stream().map(entity -> {
            PatientDepositDto dto = new PatientDepositDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        }).collect(Collectors.toList());
    }

    // --- Payment Category Methods ---
    public PaymentCategoryDto createPaymentCategory(PaymentCategoryDto dto) {
        PaymentCategory entity = new PaymentCategory();
        BeanUtils.copyProperties(dto, entity);
        PaymentCategory saved = paymentCategoryRepository.save(entity);
        dto.setId(saved.getId());
        return dto;
    }

    public List<PaymentCategoryDto> getAllPaymentCategories() {
        return paymentCategoryRepository.findAll().stream().map(entity -> {
            PaymentCategoryDto dto = new PaymentCategoryDto();
            BeanUtils.copyProperties(entity, dto);
            return dto;
        }).collect(Collectors.toList());
    }
}
