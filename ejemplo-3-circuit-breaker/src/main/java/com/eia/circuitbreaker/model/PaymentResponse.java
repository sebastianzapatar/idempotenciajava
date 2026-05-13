package com.eia.circuitbreaker.model;

import java.time.LocalDateTime;

/**
 * Modelo que representa la respuesta de un pago.
 * Incluye el estado del pago y un mensaje descriptivo.
 */
public class PaymentResponse {

    private String orderId;
    private String status;
    private String message;
    private LocalDateTime timestamp;

    public PaymentResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public PaymentResponse(String orderId, String status, String message) {
        this.orderId = orderId;
        this.status = status;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
