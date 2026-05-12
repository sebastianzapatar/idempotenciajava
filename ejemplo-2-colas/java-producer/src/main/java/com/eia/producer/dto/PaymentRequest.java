package com.eia.producer.dto;

/**
 * Record que representa la solicitud de pago enviada por el cliente al API REST.
 *
 * Este DTO se recibe en el controller y luego se serializa a JSON
 * para publicarlo en el topic Kafka "payment-requests".
 *
 * @param clientName  nombre del cliente que realiza el pago
 * @param description descripción del concepto de pago (ej: "Compra de productos")
 * @param amount      monto del pago en USD
 */
public record PaymentRequest(
        String clientName,
        String description,
        double amount
) {}
