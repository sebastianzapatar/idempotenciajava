package com.eia.producer.controller;

import com.eia.producer.dto.PaymentRequest;
import com.eia.producer.dto.PaymentResponse;
import com.eia.producer.service.PaymentProducerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST que expone el endpoint para iniciar pagos.
 *
 * Recibe una solicitud de pago con un Idempotency-Key en el header,
 * la envía a Kafka, y espera la respuesta del servicio Go.
 *
 * Si el servicio Go no responde a tiempo, el servicio reintenta
 * automáticamente con la MISMA clave de idempotencia.
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentProducerService producerService;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param producerService servicio que maneja la producción a Kafka
     */
    public PaymentController(PaymentProducerService producerService) {
        this.producerService = producerService;
    }

    /**
     * Endpoint POST para enviar un pago a través de Kafka.
     *
     * Flujo:
     * 1. Recibe el pago + Idempotency-Key.
     * 2. Publica en Kafka topic "payment-requests".
     * 3. Espera respuesta del servicio Go en "payment-responses".
     * 4. Si timeout → reintenta con la MISMA key (idempotente).
     * 5. Devuelve el resultado al cliente.
     *
     * Ejemplo con cURL:
     * <pre>
     * curl -X POST http://localhost:8081/api/payments \
     *   -H "Content-Type: application/json" \
     *   -H "Idempotency-Key: mi-uuid-unico" \
     *   -d '{"clientName":"Juan","description":"Compra laptop","amount":1500}'
     * </pre>
     *
     * @param idempotencyKey UUID único para esta transacción
     * @param request        datos del pago
     * @return respuesta con el resultado del procesamiento
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PaymentRequest request) {

        // Delegar al servicio (que maneja Kafka + reintentos + timeout)
        PaymentResponse response = producerService.sendPaymentWithRetry(idempotencyKey, request);

        // Devolver la respuesta con el código HTTP apropiado
        if ("TIMEOUT".equals(response.status())) {
            return ResponseEntity.status(504).body(response); // Gateway Timeout
        }

        return ResponseEntity.ok(response);
    }
}
