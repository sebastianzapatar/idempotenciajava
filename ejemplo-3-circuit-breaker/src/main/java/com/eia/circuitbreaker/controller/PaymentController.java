package com.eia.circuitbreaker.controller;

import com.eia.circuitbreaker.model.PaymentRequest;
import com.eia.circuitbreaker.model.PaymentResponse;
import com.eia.circuitbreaker.service.PaymentService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST para el servicio de pagos con Circuit Breaker.
 *
 * Endpoints disponibles:
 * - POST /api/payments          -> Procesar un pago (protegido por Circuit Breaker)
 * - GET  /api/payments/status   -> Consultar el estado actual del Circuit Breaker
 * - POST /api/payments/reset    -> Reiniciar manualmente el Circuit Breaker
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);
    private final PaymentService paymentService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    public PaymentController(PaymentService paymentService,
                             CircuitBreakerRegistry circuitBreakerRegistry) {
        this.paymentService = paymentService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
    }

    /**
     * Procesa un pago. Si el Circuit Breaker esta OPEN, ejecuta el fallback
     * automaticamente sin intentar la llamada al servicio externo.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("Solicitud de pago recibida para orden: {}", request.getOrderId());
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta el estado actual del Circuit Breaker.
     * Util para monitoreo y debugging en tiempo real.
     *
     * Retorna: estado (CLOSED/OPEN/HALF_OPEN), tasa de fallos,
     * llamadas totales, exitosas y fallidas.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("paymentService");
        CircuitBreaker.Metrics metrics = cb.getMetrics();

        Map<String, Object> status = Map.of(
                "state", cb.getState().name(),
                "failureRate", metrics.getFailureRate(),
                "bufferedCalls", metrics.getNumberOfBufferedCalls(),
                "successfulCalls", metrics.getNumberOfSuccessfulCalls(),
                "failedCalls", metrics.getNumberOfFailedCalls(),
                "notPermittedCalls", metrics.getNumberOfNotPermittedCalls()
        );

        log.info("Estado del Circuit Breaker: {}", status);
        return ResponseEntity.ok(status);
    }

    /**
     * Reinicia manualmente el Circuit Breaker (override manual).
     * Util cuando un administrador confirma que el servicio ya se recupero.
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetCircuitBreaker() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("paymentService");
        cb.reset();
        log.info("Circuit Breaker reiniciado manualmente");
        return ResponseEntity.ok(Map.of(
                "message", "Circuit Breaker reiniciado exitosamente",
                "newState", cb.getState().name()
        ));
    }
}
