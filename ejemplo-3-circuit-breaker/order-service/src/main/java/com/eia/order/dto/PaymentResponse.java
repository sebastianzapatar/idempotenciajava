package com.eia.order.dto;

import java.time.LocalDateTime;

/**
 * DTO (Record) para recibir respuestas del payment-service.
 * Debe coincidir con el PaymentResponse que devuelve el payment-service.
 */
public record PaymentResponse(
        String paymentId,
        String orderId,
        double amount,
        String currency,
        String status,
        String message,
        LocalDateTime processedAt
) {}
