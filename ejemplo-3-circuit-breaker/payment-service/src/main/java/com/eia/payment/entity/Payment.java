package com.eia.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa un pago procesado.
 *
 * Lombok genera automaticamente:
 * - @Data: getters, setters, toString(), equals(), hashCode()
 * - @Builder: patron Builder para construccion legible de objetos
 * - @NoArgsConstructor: constructor vacio (requerido por JPA)
 * - @AllArgsConstructor: constructor con todos los campos
 *
 * Los datos se almacenan en la tabla 'payments' de H2.
 */
@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador unico del pago (UUID generado por el servicio) */
    @Column(unique = true, nullable = false)
    private String paymentId;

    /** ID de la orden asociada (viene del order-service) */
    @Column(nullable = false)
    private String orderId;

    /** Monto del pago */
    private double amount;

    /** Moneda del pago (USD, COP, EUR) */
    private String currency;

    /** Email del cliente */
    private String customerEmail;

    /** Estado del pago: PROCESSED o REJECTED */
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /** Mensaje descriptivo del resultado */
    private String message;

    /** Momento en que se proceso el pago */
    private LocalDateTime processedAt;

    /**
     * Estados posibles de un pago.
     */
    public enum PaymentStatus {
        PROCESSED,
        REJECTED
    }
}
