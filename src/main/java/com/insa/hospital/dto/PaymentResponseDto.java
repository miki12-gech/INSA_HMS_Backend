package com.insa.hospital.dto;

import com.insa.hospital.entity.Payment;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for payment/invoice.
 *
 * Parses the legacy `category_amount` delimited string back into
 * a structured list for clean frontend consumption.
 *
 * Raw format: "88*350*diagnostic*1,90*350*diagnostic*1"
 * Parsed into: List<ParsedLineItem>
 */
public record PaymentResponseDto(
    Integer id,
    String patient,
    String patientName,
    String patientPhone,
    String patientAddress,
    String doctor,
    String doctorName,
    String date,
    String dateString,
    String category,

    /** Sub-total before discount. */
    String amount,
    String vat,
    String flatVat,
    String discount,
    String flatDiscount,
    /** Final payable amount after discount+VAT. */
    String grossTotal,

    String hospitalAmount,
    String doctorAmount,
    String amountReceived,
    String depositType,
    String status,
    String remarks,
    String user,
    String hospitalId,

    /** Raw legacy delimited string — exposed for backward compat. */
    String categoryAmountRaw,
    String categoryName,

    /** Parsed structured line items — preferred for frontend. */
    List<ParsedLineItem> items

) {

    public record ParsedLineItem(
        String categoryId,
        String price,
        String type,
        String count,
        /** Computed: price * count */
        String lineTotal
    ) {}

    public static PaymentResponseDto from(Payment p) {
        List<ParsedLineItem> items = parseLineItems(p.getCategoryAmount());
        return new PaymentResponseDto(
            p.getId(),
            p.getPatient(), p.getPatientName(), p.getPatientPhone(), p.getPatientAddress(),
            p.getDoctor(), p.getDoctorName(),
            p.getDate(), p.getDateString(),
            p.getCategory(),
            p.getAmount(),
            p.getVat(), p.getFlatVat(),
            p.getDiscount(), p.getFlatDiscount(),
            p.getGrossTotal(),
            p.getHospitalAmount(), p.getDoctorAmount(),
            p.getAmountReceived(),
            p.getDepositType(), p.getStatus(),
            p.getRemarks(), p.getUser(),
            p.getHospitalId(),
            p.getCategoryAmount(),
            p.getCategoryName(),
            items
        );
    }

    /**
     * Parses "88*350*diagnostic*1,90*350*diagnostic*1" into structured list.
     * Returns empty list if string is null/blank.
     */
    static List<ParsedLineItem> parseLineItems(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split(Payment.ITEM_SEPARATOR, -1))
            .filter(s -> !s.isBlank())
            .map(entry -> {
                String[] f = entry.split("\\*", -1);
                String catId  = f.length > 0 ? f[0].trim() : "";
                String price  = f.length > 1 ? f[1].trim() : "0";
                String type   = f.length > 2 ? f[2].trim() : "";
                String count  = f.length > 3 ? f[3].trim() : "1";
                // Compute line total
                double lineTotal = 0;
                try { lineTotal = Double.parseDouble(price) * Double.parseDouble(count); }
                catch (NumberFormatException ignored) {}
                return new ParsedLineItem(catId, price, type, count,
                        String.valueOf((long) lineTotal));
            })
            .collect(Collectors.toList());
    }
}
