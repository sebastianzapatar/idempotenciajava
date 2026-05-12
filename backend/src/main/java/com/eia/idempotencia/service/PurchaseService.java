package com.eia.idempotencia.service;

import com.eia.idempotencia.dao.PurchaseOrderDao;
import com.eia.idempotencia.dto.PurchaseRequest;
import com.eia.idempotencia.dto.PurchaseResponse;
import com.eia.idempotencia.model.PurchaseOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Servicio que contiene toda la lógica de negocio para procesar órdenes de compra.
 *
 * Aquí es donde se implementa el patrón de IDEMPOTENCIA:
 *
 * 1. Se recibe una solicitud de compra junto con un Idempotency-Key (UUID).
 * 2. Se consulta en la base de datos si ya existe una orden con esa clave.
 * 3. Si ya existe: se devuelve el resultado anterior SIN procesar de nuevo.
 *    Esto evita cobros dobles, registros duplicados, etc.
 * 4. Si no existe: se procesa la compra, se guarda en la DB, y se devuelve el resultado.
 *
 * Toda la operación es TRANSACCIONAL para garantizar atomicidad.
 */
@Service
public class PurchaseService {

    // Inyección del DAO (repositorio) para acceder a la base de datos
    private final PurchaseOrderDao purchaseOrderDao;

    /**
     * Constructor con inyección de dependencias.
     * Spring inyecta automáticamente la implementación del DAO.
     *
     * @param purchaseOrderDao repositorio JPA de órdenes de compra
     */
    public PurchaseService(PurchaseOrderDao purchaseOrderDao) {
        this.purchaseOrderDao = purchaseOrderDao;
    }

    /**
     * Procesa una solicitud de compra de forma IDEMPOTENTE.
     *
     * Flujo:
     * - Busca si ya existe una orden con el mismo Idempotency-Key.
     * - Si existe: devuelve el resultado anterior (alreadyProcessed = true).
     * - Si no existe: crea la orden, la guarda y devuelve el resultado nuevo.
     *
     * La anotación @Transactional asegura que la operación de guardar
     * la orden y el Idempotency-Key se hagan de forma atómica.
     * Si ocurre un error, toda la transacción se revierte.
     *
     * @param idempotencyKey clave UUID única de la transacción
     * @param request        datos de la compra
     * @return PurchaseResponse con la información de la orden
     */
    @Transactional
    public PurchaseResponse processPurchase(String idempotencyKey, PurchaseRequest request) {

        // PASO 1: Verificar si la clave de idempotencia ya fue procesada
        Optional<PurchaseOrder> existingOrder = purchaseOrderDao.findByIdempotencyKey(idempotencyKey);

        if (existingOrder.isPresent()) {
            // ¡IDEMPOTENCIA ACTIVADA!
            // La solicitud ya fue procesada antes.
            // Devolvemos el resultado original sin volver a procesar.
            PurchaseOrder order = existingOrder.get();
            return toResponse(order, true);
        }

        // PASO 2: La clave no existe, es una solicitud nueva.
        // Creamos y guardamos la orden de compra.
        PurchaseOrder newOrder = new PurchaseOrder(
                idempotencyKey,
                request.clientName(),      // Acceso a campos de record con nombre()
                request.productDetails(),
                request.totalAmount(),
                "COMPLETED"                // Estado inicial: COMPLETED
        );

        // Guardamos en la base de datos
        PurchaseOrder savedOrder = purchaseOrderDao.save(newOrder);

        // Devolvemos la respuesta indicando que es una solicitud nueva
        return toResponse(savedOrder, false);
    }

    /**
     * Obtiene todas las órdenes de compra registradas.
     * Útil para verificar que no haya duplicados en la base de datos.
     *
     * @return lista de todas las órdenes
     */
    public List<PurchaseResponse> getAllPurchases() {
        return purchaseOrderDao.findAll()
                .stream()
                .map(order -> toResponse(order, false))
                .toList();
    }

    /**
     * Convierte una entidad PurchaseOrder en un DTO PurchaseResponse.
     *
     * @param order            la entidad de base de datos
     * @param alreadyProcessed flag que indica si la solicitud ya existía
     * @return el DTO de respuesta
     */
    private PurchaseResponse toResponse(PurchaseOrder order, boolean alreadyProcessed) {
        return new PurchaseResponse(
                order.getId(),
                order.getIdempotencyKey(),
                order.getClientName(),
                order.getProductDetails(),
                order.getTotalAmount(),
                order.getStatus(),
                alreadyProcessed
        );
    }
}
