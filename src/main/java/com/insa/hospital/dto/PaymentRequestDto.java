package com.insa.hospital.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO for creating a payment/invoice.
 *
 * Accepts main invoice fields PLUS a list of structured line items.
 * The service layer:
 *   1. Validates and calculates server-side totals (never trusts frontend totals)
 *   2. Serializes line items → legacy `category_amount` delimited string
 *   3. Applies commission split (hospital_amount / doctor_amount)
 *
 * hospitalId, user (cashier), patient_name, doctor_name are resolved server-side.
 */
public record PaymentRequestDto(

    /** References patient.id (varchar). Required. */
    @NotBlank(message = "Patient is required")
    @Size(max = 100)
    String patient,

    /** References doctor.id (varchar). May be "0" or blank. */
    @Size(max = 100)
    String doctor,

    /**
     * Structured line items for this invoice.
     * The service serializes these into: "catId*price*type*count,..."
     * Must have at least one item.
     */
    @Valid
    List<@Valid PaymentLineItemDto> items,

    /**
     * Discount percentage applied to sub-total (e.g. "10" = 10%).
     * DEFAULT "0" if null/blank.
     */
    @Size(max = 100)
    String discount,

    /** Fixed flat discount amount (e.g. "50"). Overrides percentage if set. */
    @Size(max = 100)
    String flatDiscount,

    /** VAT percentage (e.g. "0", "15"). Default "0". */
    @Size(max = 100)
    String vat,

    /** Payment method: 'Cash', 'Online', 'Card'. Default 'Cash'. */
    @Size(max = 100)
    String depositType,

    /** Amount received from patient so far. */
    @Size(max = 100)
    String amountReceived,

    /** Cashier's remarks (optional). */
    @Size(max = 500)
    String remarks

) {

    /**
     * One line item on the invoice.
     * Maps to: paymentCategoryId*price*type*count
     */
    public record PaymentLineItemDto(

        /** payment_category.id for this service. */
        @NotNull(message = "Item category ID is required")
        Integer categoryId,

        /** Unit price for this item (override or from catalogue). */
        @NotBlank(message = "Price is required")
        @Size(max = 100)
        String price,

        /** Category type from payment_category.type: 'diagnostic' | 'others'. */
        @Size(max = 100)
        String type,

        /** Quantity / count of this item (default 1). */
        @NotNull
        @PositiveOrZero
        Integer count

    ) {}
}
