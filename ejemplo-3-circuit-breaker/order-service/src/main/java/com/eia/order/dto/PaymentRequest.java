package com.eia.order.dto;

/**
 * DTO (Record) para enviar solicitudes de pago al payment-service.
 * Debe coincidir con el PaymentRequest que espera el payment-service.
 */
public record PaymentRequest(
        String orderId,
        double amount,
        String currency,
        String customerEmail
) {}
