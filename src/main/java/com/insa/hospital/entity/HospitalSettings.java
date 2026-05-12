package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity mapping the legacy `settings` table.
 *
 * ══════════════════════════════════════════════════════════════
 *  AUDIT FIX — Alert 1: Settings table vs Hospital table
 * ══════════════════════════════════════════════════════════════
 *  The legacy settings_model.php queries the `settings` table —
 *  NOT the `hospital` table. The `hospital` table is the SaaS
 *  tenant registry; the `settings` table holds per-hospital
 *  operational configuration (branding, currency, gateways).
 *
 *  Column names derived from settings/controllers/settings.php
 *  (lines 112–133) and settings/models/settings_model.php:
 *    system_vendor, title, email, address, phone,
 *    currency, logo, language, payment_gateway, sms_gateway,
 *    discount_type, codec_username, codec_purchase_code
 *
 * NOTE: hospital_id is the tenant FK. Queried by hospital_id,
 *       identical to all other tables in the legacy schema.
 * ══════════════════════════════════════════════════════════════
 */
@Entity
@Table(name = "settings")
@Getter
@Setter
@NoArgsConstructor
public class HospitalSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    /** Hospital/system branding name (displayed in UI header). */
    @Column(name = "system_vendor", length = 500)
    private String systemVendor;

    /** Page title shown in browser tab. */
    @Column(name = "title", length = 500)
    private String title;

    /** Contact email for this hospital. */
    @Column(name = "email", length = 500)
    private String email;

    /** Physical address of the hospital. */
    @Column(name = "address", length = 500)
    private String address;

    /** Contact phone number. */
    @Column(name = "phone", length = 100)
    private String phone;

    /** ISO currency symbol (e.g. "ETB", "USD", "NGN"). Max 3 chars. */
    @Column(name = "currency", length = 10)
    private String currency;

    /** Path to hospital logo image (relative to uploads/ dir). */
    @Column(name = "logo", length = 1000)
    private String logo;

    /** Active UI language (e.g. "english", "arabic"). */
    @Column(name = "language", length = 100)
    private String language;

    /**
     * Active online payment gateway: "PayPal" | "Pay U Money".
     * Used in finance/controllers/finance.php line 312.
     */
    @Column(name = "payment_gateway", length = 100)
    private String paymentGateway;

    /** Active SMS gateway: "Nexmo" | "Twilio" | "SSLCommerz". */
    @Column(name = "sms_gateway", length = 100)
    private String smsGateway;

    /**
     * Discount calculation mode: "flat" or "percentage".
     * Used in PaymentService to choose between fixed deduction
     * and percentage-based deduction.
     */
    @Column(name = "discount_type", length = 50)
    private String discountType;

    /**
     * Marketplace buyer username (SaaS licensing field).
     * Not used in patient-facing logic.
     */
    @Column(name = "codec_username", length = 500)
    private String codecUsername;

    /**
     * Marketplace purchase code (SaaS licensing field).
     * Not used in patient-facing logic.
     */
    @Column(name = "codec_purchase_code", length = 100)
    private String codecPurchaseCode;

    /** Multi-tenant hospital identifier. FK to hospital.id. */
    @Column(name = "hospital_id", length = 100)
    private String hospitalId;
}
