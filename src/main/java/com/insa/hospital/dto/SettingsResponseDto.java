package com.insa.hospital.dto;

import com.insa.hospital.entity.Settings;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SettingsResponseDto {
    private Long id;
    private String systemVendor;
    private String title;
    private String address;
    private String phone;
    private String email;
    private String facebookId;
    private String currency;
    private String language;
    private String discount;
    private String vat;
    private String loginTitle;
    private String logo;
    private String invoiceLogo;
    private String paymentGateway;
    private String smsGateway;
    private String codecUsername;
    private String codecPurchaseCode;
    private String hospitalId;

    public SettingsResponseDto(Settings settings) {
        if (settings != null) {
            this.id = settings.getId();
            this.systemVendor = settings.getSystemVendor();
            this.title = settings.getTitle();
            this.address = settings.getAddress();
            this.phone = settings.getPhone();
            this.email = settings.getEmail();
            this.facebookId = settings.getFacebookId();
            this.currency = settings.getCurrency();
            this.language = settings.getLanguage();
            this.discount = settings.getDiscount();
            this.vat = settings.getVat();
            this.loginTitle = settings.getLoginTitle();
            this.logo = settings.getLogo();
            this.invoiceLogo = settings.getInvoiceLogo();
            this.paymentGateway = settings.getPaymentGateway();
            this.smsGateway = settings.getSmsGateway();
            this.codecUsername = settings.getCodecUsername();
            this.codecPurchaseCode = settings.getCodecPurchaseCode();
            this.hospitalId = settings.getHospitalId();
        }
    }
}
