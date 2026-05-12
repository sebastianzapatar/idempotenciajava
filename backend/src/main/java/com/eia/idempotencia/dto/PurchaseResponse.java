package com.eia.idempotencia.dto;

/**
 * Record que representa la respuesta enviada al cliente tras una compra.
 *
 * Contiene la información relevante de la orden procesada, incluyendo
 * un campo booleano 'alreadyProcessed' que indica si la solicitud ya había
 * sido procesada previamente (idempotencia activada).
 *
 * @param id               identificador único de la orden en la base de datos
 * @param idempotencyKey   clave UUID utilizada para la idempotencia
 * @param clientName       nombre del cliente
 * @param productDetails   descripción de los productos
 * @param totalAmount      monto total
 * @param status           estado de la orden (COMPLETED, FAILED, etc.)
 * @param alreadyProcessed true si la solicitud ya había sido procesada previamente
 */
public record PurchaseResponse(
        Long id,
        String idempotencyKey,
        String clientName,
        String productDetails,
        Double totalAmount,
        String status,
        boolean alreadyProcessed
) {}
