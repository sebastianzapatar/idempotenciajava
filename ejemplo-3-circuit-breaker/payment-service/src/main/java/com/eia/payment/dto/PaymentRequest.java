package com.eia.payment.dto;

/**
 * DTO (Record) para recibir solicitudes de pago.
 *
 * Java Records son inmutables y generan automaticamente:
 * - Constructor con todos los campos
 * - Getters (metodos con el nombre del campo: orderId(), amount(), etc.)
 * - toString(), equals(), hashCode()
 *
 * Son ideales para DTOs porque son concisos y thread-safe.
 */
public record PaymentRequest(
        /** ID de la orden que origina el pago */
        String orderId,
        /** Monto a cobrar */
        double amount,
        /** Moneda (USD, COP, EUR) */
        String currency,
        /** Email del cliente */
        String customerEmail
) {}
