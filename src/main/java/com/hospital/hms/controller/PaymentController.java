package com.hospital.hms.controller;

import com.hospital.hms.dto.OtPaymentDto;
import com.hospital.hms.dto.PatientDepositDto;
import com.hospital.hms.dto.PaymentCategoryDto;
import com.hospital.hms.dto.PaymentDto;
import com.hospital.hms.service.PaymentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/finance")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/payment")
    public ResponseEntity<PaymentDto> createPayment(@RequestBody PaymentDto dto) {
        return ResponseEntity.ok(paymentService.createPayment(dto));
    }

    @GetMapping("/payment")
    public ResponseEntity<List<PaymentDto>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PostMapping("/ot-payment")
    public ResponseEntity<OtPaymentDto> createOtPayment(@RequestBody OtPaymentDto dto) {
        return ResponseEntity.ok(paymentService.createOtPayment(dto));
    }

    @GetMapping("/ot-payment")
    public ResponseEntity<List<OtPaymentDto>> getAllOtPayments() {
        return ResponseEntity.ok(paymentService.getAllOtPayments());
    }

    @PostMapping("/deposit")
    public ResponseEntity<PatientDepositDto> createPatientDeposit(@RequestBody PatientDepositDto dto) {
        return ResponseEntity.ok(paymentService.createPatientDeposit(dto));
    }

    @GetMapping("/deposit")
    public ResponseEntity<List<PatientDepositDto>> getAllPatientDeposits() {
        return ResponseEntity.ok(paymentService.getAllPatientDeposits());
    }

    @PostMapping("/category")
    public ResponseEntity<PaymentCategoryDto> createPaymentCategory(@RequestBody PaymentCategoryDto dto) {
        return ResponseEntity.ok(paymentService.createPaymentCategory(dto));
    }

    @GetMapping("/category")
    public ResponseEntity<List<PaymentCategoryDto>> getAllPaymentCategories() {
        return ResponseEntity.ok(paymentService.getAllPaymentCategories());
    }
}
