package com.eia.circuitbreaker.model;

/**
 * Modelo que representa una solicitud de pago.
 * Contiene los datos necesarios para procesar un pago.
 */
public class PaymentRequest {

    private String orderId;
    private double amount;
    private String currency;
    private String customerEmail;

    public PaymentRequest() {}

    public PaymentRequest(String orderId, double amount, String currency, String customerEmail) {
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.customerEmail = customerEmail;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
}
