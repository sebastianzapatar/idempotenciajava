package com.eia.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de Pagos.
 *
 * Este servicio es responsable de procesar pagos y almacenarlos en H2.
 * Se registra en Eureka para ser descubierto por el order-service.
 *
 * Incluye un endpoint para simular caida del servicio (/api/payments/toggle),
 * lo que permite demostrar el Circuit Breaker en el order-service.
 *
 * Endpoints:
 * - POST /api/payments       -> Procesar un pago
 * - GET  /api/payments       -> Listar todos los pagos
 * - POST /api/payments/toggle -> Activar/desactivar el servicio (simulacion)
 * - GET  /api/payments/health -> Ver si el servicio esta disponible
 *
 * H2 Console: http://localhost:8082/h2-console (JDBC URL: jdbc:h2:mem:paymentdb)
 *
 * Universidad EIA - Ingenieria de Sistemas y Computacion - 2026
 */
@SpringBootApplication
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
