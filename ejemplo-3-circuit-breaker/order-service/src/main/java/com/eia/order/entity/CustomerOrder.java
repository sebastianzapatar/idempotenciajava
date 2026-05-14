package com.eia.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una orden de compra.
 *
 * Ciclo de vida de una orden:
 * 1. CREATED  -> La orden fue recibida y guardada
 * 2. PAID     -> El payment-service confirmo el pago exitosamente
 * 3. PAYMENT_FAILED  -> El payment-service rechazo el pago
 * 4. PAYMENT_PENDING -> Circuit Breaker activo, pago queda pendiente
 *
 * Se usa la tabla 'customer_orders' porque 'ORDER' es palabra reservada en SQL.
 *
 * Lombok:
 * - @Data: genera getters, setters, toString, equals, hashCode
 * - @Builder: patron Builder para construccion legible
 * - @NoArgsConstructor: constructor vacio (requerido por JPA)
 * - @AllArgsConstructor: constructor con todos los campos
 */
@Entity
@Table(name = "customer_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador unico de la orden (UUID) */
    @Column(unique = true, nullable = false)
    private String orderId;

    /** Nombre del cliente */
    @Column(nullable = false)
    private String customerName;

    /** Email del cliente */
    private String customerEmail;

    /** Nombre del producto o servicio */
    private String productName;

    /** Monto de la orden */
    private double amount;

    /** Moneda (USD, COP, EUR) */
    private String currency;

    /** Estado actual de la orden */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    /** ID del pago asociado (asignado por payment-service) */
    private String paymentId;

    /** Mensaje descriptivo del estado */
    @Column(length = 500)
    private String statusMessage;

    /** Fecha de creacion de la orden */
    private LocalDateTime createdAt;

    /** Fecha de ultima actualizacion */
    private LocalDateTime updatedAt;

    /**
     * Estados posibles de una orden.
     */
    public enum OrderStatus {
        /** Orden recien creada, pago aun no procesado */
        CREATED,
        /** Pago confirmado exitosamente por payment-service */
        PAID,
        /** Pago rechazado por payment-service */
        PAYMENT_FAILED,
        /** Circuit Breaker activo, pago queda pendiente */
        PAYMENT_PENDING
    }
}
