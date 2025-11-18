package com.racephotos.domain.photographer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Stores the payout channel metadata configured by a photographer.
 */
@Embeddable
public class PayoutPreferences {

    @Enumerated(EnumType.STRING)
    @Column(name = "payout_method", length = 30)
    private PayoutMethod method = PayoutMethod.UNSPECIFIED;

    @Column(name = "payout_account_reference", length = 255)
    private String accountReference;

    @Column(name = "payout_email", length = 160)
    private String payoutEmail;

    @Column(name = "bank_account_last4", length = 4)
    private String bankAccountLast4;

    @Column(name = "bank_routing_number", length = 9)
    private String bankRoutingNumber;

    @Column(name = "payout_tax_id", length = 64)
    private String taxId;

    @Column(name = "payout_metadata", columnDefinition = "TEXT")
    private String metadata;

    public PayoutPreferences() {
    }

    public PayoutMethod getMethod() {
        return method;
    }

    public void setMethod(PayoutMethod method) {
        this.method = method;
    }

    public String getAccountReference() {
        return accountReference;
    }

    public void setAccountReference(String accountReference) {
        this.accountReference = accountReference;
    }

    public String getPayoutEmail() {
        return payoutEmail;
    }

    public void setPayoutEmail(String payoutEmail) {
        this.payoutEmail = payoutEmail;
    }

    public String getBankAccountLast4() {
        return bankAccountLast4;
    }

    public void setBankAccountLast4(String bankAccountLast4) {
        this.bankAccountLast4 = bankAccountLast4;
    }

    public String getBankRoutingNumber() {
        return bankRoutingNumber;
    }

    public void setBankRoutingNumber(String bankRoutingNumber) {
        this.bankRoutingNumber = bankRoutingNumber;
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
