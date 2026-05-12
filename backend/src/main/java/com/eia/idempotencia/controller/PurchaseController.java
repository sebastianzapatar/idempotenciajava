package com.eia.idempotencia.controller;

import com.eia.idempotencia.dto.PurchaseRequest;
import com.eia.idempotencia.dto.PurchaseResponse;
import com.eia.idempotencia.service.PurchaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST que expone los endpoints para el sistema de compras idempotente.
 *
 * El controlador solo se encarga de:
 * 1. Recibir la solicitud HTTP
 * 2. Extraer los parámetros (headers, body)
 * 3. Delegar la lógica de negocio al servicio (PurchaseService)
 * 4. Devolver la respuesta HTTP apropiada
 *
 * NO contiene lógica de negocio (eso va en el Service).
 * NO accede directamente a la base de datos (eso va en el DAO).
 */
@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*") // Permitir solicitudes del frontend React
public class PurchaseController {

    // Inyección del servicio que contiene la lógica de negocio
    private final PurchaseService purchaseService;

    /**
     * Constructor con inyección de dependencias.
     * Spring inyecta automáticamente la implementación del servicio.
     *
     * @param purchaseService servicio de lógica de compras idempotentes
     */
    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * Endpoint POST para crear una nueva orden de compra.
     *
     * IDEMPOTENCIA: El cliente debe enviar un header "Idempotency-Key" con un UUID único.
     * - Si el UUID ya fue procesado: se devuelve el resultado original (HTTP 200).
     * - Si el UUID es nuevo: se procesa la compra y se devuelve HTTP 201.
     *
     * Ejemplo de uso con cURL:
     * <pre>
     * curl -X POST http://localhost:8080/api/purchases \
     *   -H "Content-Type: application/json" \
     *   -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
     *   -d '{"clientName":"Juan","productDetails":"Laptop, Mouse","totalAmount":1500.50}'
     * </pre>
     *
     * @param idempotencyKey header UUID que identifica de forma única esta transacción
     * @param request        cuerpo con los datos de la compra
     * @return ResponseEntity con la orden creada o la existente
     */
    @PostMapping
    public ResponseEntity<PurchaseResponse> createPurchase(
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @RequestBody PurchaseRequest request) {

        // Delegamos toda la lógica al servicio
        PurchaseResponse response = purchaseService.processPurchase(idempotencyKey, request);

        if (response.alreadyProcessed()) {
            // La solicitud ya fue procesada anteriormente.
            // Devolvemos HTTP 200 (OK) con el resultado original.
            // Esto es IDEMPOTENCIA en acción.
            return ResponseEntity.status(HttpStatus.OK).body(response);
        }

        // La solicitud es nueva y fue procesada exitosamente.
        // Devolvemos HTTP 201 (CREATED).
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint GET para listar todas las órdenes de compra.
     *
     * GET es idempotente por naturaleza: llamarlo múltiples veces
     * siempre devuelve el mismo resultado sin alterar el estado del servidor.
     *
     * Útil para verificar visualmente que no se hayan creado registros duplicados.
     *
     * @return lista de todas las órdenes de compra registradas
     */
    @GetMapping
    public ResponseEntity<List<PurchaseResponse>> getAllPurchases() {
        return ResponseEntity.ok(purchaseService.getAllPurchases());
    }
}
