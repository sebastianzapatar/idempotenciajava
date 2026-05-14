package com.eia.order.controller;

import com.eia.order.dto.CreateOrderRequest;
import com.eia.order.dto.OrderResponse;
import com.eia.order.service.OrderService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST del microservicio de ordenes.
 *
 * Expone endpoints para:
 * - Crear ordenes (con pago procesado via Circuit Breaker)
 * - Listar ordenes almacenadas en H2
 * - Consultar estado del Circuit Breaker
 * - Reiniciar el Circuit Breaker manualmente
 *
 * Lombok:
 * - @Slf4j: logger automatico
 * - @RequiredArgsConstructor: inyeccion por constructor de campos final
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    /**
     * Crea una nueva orden y procesa el pago.
     *
     * Si el payment-service esta disponible -> orden queda PAID
     * Si el Circuit Breaker esta OPEN -> fallback -> orden queda PAYMENT_PENDING
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody CreateOrderRequest request) {
        log.info("Solicitud de orden recibida - Cliente: {}, Producto: {}, Monto: {} {}",
                request.customerName(), request.productName(), request.amount(), request.currency());
        OrderResponse response = orderService.createOrder(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todas las ordenes almacenadas en H2.
     * Muestra el historial de ordenes con sus estados de pago.
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    /**
     * Consulta el estado actual del Circuit Breaker.
     *
     * Retorna metricas en tiempo real:
     * - state: CLOSED, OPEN, HALF_OPEN
     * - failureRate: porcentaje de fallos
     * - bufferedCalls: total de llamadas en la ventana
     * - successfulCalls / failedCalls: desglose de resultados
     * - notPermittedCalls: llamadas rechazadas por CB en estado OPEN
     */
    @GetMapping("/cb/status")
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
     * Reinicia manualmente el Circuit Breaker.
     * Vuelve al estado CLOSED con metricas en cero.
     */
    @PostMapping("/cb/reset")
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
