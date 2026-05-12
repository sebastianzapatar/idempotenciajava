package com.eia.producer.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo que representa un evento de pago que viaja por Kafka.
 *
 * Este objeto se serializa a JSON y se publica en el topic "payment-requests".
 * El servicio Go lo deserializa, procesa el pago, y devuelve una respuesta
 * en el topic "payment-responses".
 *
 * La clave de idempotencia (idempotencyKey) es la pieza central:
 * el servicio Go la usa para verificar si el pago ya fue procesado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    /** Clave UUID única que identifica esta transacción (Idempotency-Key) */
    private String idempotencyKey;

    /** Nombre del cliente */
    private String clientName;

    /** Descripción del pago */
    private String description;

    /** Monto del pago */
    private double amount;

    /** Marca de tiempo en la que se generó el evento (epoch millis) */
    private long timestamp;
}
