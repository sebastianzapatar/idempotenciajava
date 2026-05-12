package com.eia.idempotencia.dto;

/**
 * Record que representa la solicitud de compra enviada por el cliente.
 *
 * Los records de Java son clases inmutables que generan automáticamente:
 * - Constructor con todos los campos
 * - Métodos de acceso (getters sin el prefijo "get")
 * - equals(), hashCode() y toString()
 *
 * Esto es más limpio y conciso que una clase POJO tradicional para DTOs,
 * ya que un DTO (Data Transfer Object) solo transporta datos y no necesita
 * lógica adicional.
 *
 * @param clientName     nombre del cliente que realiza la compra
 * @param productDetails descripción de los productos (ej: "Laptop, Ratón, Teclado")
 * @param totalAmount    monto total de la compra
 */
public record PurchaseRequest(
        String clientName,
        String productDetails,
        Double totalAmount
) {}
