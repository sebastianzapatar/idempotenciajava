package com.eia.payment.dto;

import java.time.LocalDateTime;

/**
 * DTO (Record) para respuestas de pago.
 *
 * Contiene toda la informacion del pago procesado que se devuelve
 * al order-service despues de procesar (o rechazar) el pago.
 */
public record PaymentResponse(
        /** Identificador unico del pago generado */
        String paymentId,
        /** ID de la orden asociada */
        String orderId,
        /** Monto procesado */
        double amount,
        /** Moneda */
        String currency,
        /** Estado: PROCESSED o REJECTED */
        String status,
        /** Mensaje descriptivo del resultado */
        String message,
        /** Momento del procesamiento */
        LocalDateTime processedAt
) {}
