package com.insa.hospital.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "settings")
@Data
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "system_vendor")
    private String systemVendor;

    private String title;

    private String address;

    private String phone;

    private String email;

    @Column(name = "facebook_id")
    private String facebookId;

    private String currency;

    private String language;

    private String discount;

    private String vat;

    @Column(name = "login_title")
    private String loginTitle;

    private String logo;

    @Column(name = "invoice_logo")
    private String invoiceLogo;

    @Column(name = "homepage_title")
    private String homepageTitle;

    @Column(name = "homepage_description", columnDefinition = "TEXT")
    private String homepageDescription;

    @Column(name = "footer_text", columnDefinition = "TEXT")
    private String footerText;

    @Column(name = "footer_tagline")
    private String footerTagline;

    @Column(name = "primary_color")
    private String primaryColor;

    @Column(name = "primary_dark_color")
    private String primaryDarkColor;

    @Column(name = "accent_color")
    private String accentColor;

    @Column(name = "background_color")
    private String backgroundColor;

    @Column(name = "surface_color")
    private String surfaceColor;

    @Column(name = "text_color")
    private String textColor;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "sms_gateway")
    private String smsGateway;

    @Column(name = "codec_username")
    private String codecUsername;

    @Column(name = "codec_purchase_code")
    private String codecPurchaseCode;

    @Column(name = "hospital_id")
    private String hospitalId;
}
