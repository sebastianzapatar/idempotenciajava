package com.eia.idempotencia.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad JPA que representa una orden de compra en el sistema.
 *
 * Esta entidad almacena la información de cada compra realizada por un cliente,
 * incluyendo la clave de idempotencia que garantiza que una misma solicitud
 * no sea procesada más de una vez.
 *
 * La columna 'idempotencyKey' tiene un constraint UNIQUE para que la base de datos
 * rechace intentos de inserción duplicada a nivel de persistencia.
 *
 * Lombok genera automáticamente:
 * - @Data       → getters, setters, equals(), hashCode(), toString()
 * - @NoArgsConstructor  → constructor vacío requerido por JPA
 * - @AllArgsConstructor → constructor con todos los campos
 */
@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    /** Identificador autoincremental de la orden */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Clave de idempotencia (UUID) que identifica de forma única esta transacción.
     * Debe ser única y no nula para garantizar que la misma solicitud
     * no se procese más de una vez.
     */
    @Column(unique = true, nullable = false)
    private String idempotencyKey;

    /** Nombre del cliente que realiza la compra */
    private String clientName;

    /** Descripción de los productos comprados */
    private String productDetails;

    /** Monto total de la compra */
    private Double totalAmount;

    /** Estado de la orden: COMPLETED, PENDING, FAILED */
    private String status;

    /**
     * Constructor personalizado sin el ID (que es autogenerado por la DB).
     * Útil para crear nuevas órdenes desde el servicio.
     *
     * @param idempotencyKey clave UUID única de la transacción
     * @param clientName     nombre del cliente
     * @param productDetails detalle de los productos
     * @param totalAmount    monto total
     * @param status         estado de la orden
     */
    public PurchaseOrder(String idempotencyKey, String clientName,
                         String productDetails, Double totalAmount, String status) {
        this.idempotencyKey = idempotencyKey;
        this.clientName = clientName;
        this.productDetails = productDetails;
        this.totalAmount = totalAmount;
        this.status = status;
    }
}
