package com.eia.producer.dto;

/**
 * Record que representa la respuesta del procesamiento de un pago.
 *
 * Este DTO se construye a partir de la respuesta recibida del servicio Go
 * a través del topic Kafka "payment-responses".
 *
 * @param idempotencyKey   clave UUID de la transacción
 * @param status           estado del procesamiento: "PROCESSED", "DUPLICATE", "TIMEOUT"
 * @param message          mensaje descriptivo del resultado
 * @param processedByGo    true si fue procesado exitosamente por el servicio Go
 * @param retriesUsed      número de reintentos que fueron necesarios
 */
public record PaymentResponse(
        String idempotencyKey,
        String status,
        String message,
        boolean processedByGo,
        int retriesUsed
) {}
