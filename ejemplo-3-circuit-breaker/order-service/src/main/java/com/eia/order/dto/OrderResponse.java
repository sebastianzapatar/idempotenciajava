package com.eia.order.dto;

import java.time.LocalDateTime;

/**
 * DTO (Record) para respuestas de orden enviadas al frontend.
 * Incluye toda la informacion relevante de la orden y su estado de pago.
 */
public record OrderResponse(
        Long id,
        String orderId,
        String customerName,
        String customerEmail,
        String productName,
        double amount,
        String currency,
        String status,
        String paymentId,
        String statusMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
