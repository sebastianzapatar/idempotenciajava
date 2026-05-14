package com.eia.payment.controller;

import com.eia.payment.dto.PaymentRequest;
import com.eia.payment.dto.PaymentResponse;
import com.eia.payment.service.PaymentProcessorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST del microservicio de pagos.
 *
 * Expone los endpoints para:
 * - Procesar pagos (llamado por order-service via Feign/HTTP)
 * - Listar pagos almacenados en H2
 * - Toggle de disponibilidad (para demostrar Circuit Breaker)
 * - Health check de disponibilidad
 *
 * Lombok:
 * - @Slf4j: logger automatico
 * - @RequiredArgsConstructor: inyeccion por constructor de campos final
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentProcessorService paymentService;

    /**
     * Procesa un pago recibido del order-service.
     * Si el servicio esta desactivado, lanza excepcion (500),
     * lo que el Circuit Breaker del order-service detecta como fallo.
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        log.info("Solicitud de pago recibida - Orden: {}, Monto: {} {}",
                request.orderId(), request.amount(), request.currency());
        PaymentResponse response = paymentService.processPayment(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lista todos los pagos almacenados en la base de datos H2.
     * Util para verificar que los pagos se persisten correctamente.
     */
    @GetMapping
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    /**
     * Alterna la disponibilidad del servicio (ON/OFF).
     *
     * Cuando se desactiva, todas las peticiones POST /api/payments
     * lanzaran excepcion, simulando un servicio caido.
     * Esto permite probar el Circuit Breaker sin apagar el contenedor.
     */
    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleAvailability() {
        boolean available = paymentService.toggleAvailability();
        log.warn("Toggle ejecutado - Servicio ahora: {}", available ? "ACTIVO" : "DESACTIVADO");
        return ResponseEntity.ok(Map.of(
                "available", available,
                "message", available
                        ? "Servicio de pagos ACTIVADO - procesara pagos normalmente"
                        : "Servicio de pagos DESACTIVADO - rechazara todas las peticiones"
        ));
    }

    /**
     * Health check: indica si el servicio esta disponible.
     * El frontend usa este endpoint para mostrar el estado actual.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        boolean available = paymentService.isAvailable();
        return ResponseEntity.ok(Map.of(
                "service", "payment-service",
                "available", available,
                "status", available ? "UP" : "DOWN (simulado)"
        ));
    }
}
