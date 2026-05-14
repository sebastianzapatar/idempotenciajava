package com.eia.order.client;

import com.eia.order.dto.PaymentRequest;
import com.eia.order.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Cliente Feign declarativo para comunicarse con el payment-service.
 *
 * OpenFeign genera automaticamente la implementacion HTTP a partir
 * de esta interfaz. Usa Eureka para resolver el nombre "payment-service"
 * a la direccion IP y puerto reales del servicio.
 *
 * Flujo interno:
 * 1. order-service llama a paymentClient.processPayment(request)
 * 2. Feign consulta Eureka: "Donde esta payment-service?"
 * 3. Eureka responde: "Esta en 172.18.0.4:8082"
 * 4. Feign hace POST http://172.18.0.4:8082/api/payments con el body
 * 5. Feign deserializa la respuesta en PaymentResponse
 *
 * Si payment-service esta caido o devuelve error, Feign lanza FeignException,
 * que el Circuit Breaker en OrderService intercepta.
 *
 * @FeignClient(name) debe coincidir con spring.application.name del servicio destino
 */
@FeignClient(name = "payment-service")
public interface PaymentClient {

    /**
     * Envia una solicitud de pago al payment-service.
     *
     * @param request datos del pago
     * @return respuesta del payment-service con el resultado del pago
     */
    @PostMapping("/api/payments")
    PaymentResponse processPayment(@RequestBody PaymentRequest request);
}
