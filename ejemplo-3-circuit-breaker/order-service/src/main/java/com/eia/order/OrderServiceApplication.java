package com.eia.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Microservicio de Ordenes con Circuit Breaker.
 *
 * Este servicio recibe solicitudes de ordenes desde el frontend,
 * las persiste en H2, y llama al payment-service via OpenFeign
 * para procesar el pago. La llamada al payment-service esta
 * protegida por un Circuit Breaker (Resilience4j).
 *
 * @EnableFeignClients: habilita el escaneo de interfaces Feign
 * para generar automaticamente los clientes HTTP.
 *
 * Flujo:
 * 1. Frontend envia orden al order-service
 * 2. order-service guarda la orden con status CREATED
 * 3. order-service llama al payment-service via Feign (protegido con CB)
 * 4. Si payment-service responde OK -> orden pasa a PAID
 * 5. Si payment-service falla y CB se activa -> fallback -> PAYMENT_PENDING
 *
 * Endpoints:
 * - POST /api/orders         -> Crear orden y procesar pago
 * - GET  /api/orders         -> Listar todas las ordenes
 * - GET  /api/orders/cb/status -> Estado del Circuit Breaker
 * - POST /api/orders/cb/reset  -> Reiniciar Circuit Breaker
 *
 * H2 Console: http://localhost:8081/h2-console (JDBC URL: jdbc:h2:mem:orderdb)
 *
 * Universidad EIA - Ingenieria de Sistemas y Computacion - 2026
 */
@EnableFeignClients
@SpringBootApplication
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
