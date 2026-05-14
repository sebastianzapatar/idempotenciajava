package com.eia.order.service;

import com.eia.order.client.PaymentClient;
import com.eia.order.dto.*;
import com.eia.order.entity.CustomerOrder;
import com.eia.order.entity.CustomerOrder.OrderStatus;
import com.eia.order.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de ordenes con Circuit Breaker.
 *
 * Orquesta la creacion de ordenes y la comunicacion con el payment-service.
 * La llamada al payment-service esta protegida por @CircuitBreaker de Resilience4j.
 *
 * Cuando el payment-service falla repetidamente:
 * 1. Las primeras llamadas fallan normalmente (estado CLOSED)
 * 2. Al superar el umbral de fallos (50%), el circuito se ABRE
 * 3. En estado OPEN, las llamadas ejecutan el fallback sin intentar la conexion
 * 4. Despues de 30s, pasa a HALF-OPEN para probar con 3 llamadas
 * 5. Si las pruebas son exitosas, vuelve a CLOSED
 *
 * Lombok:
 * - @Slf4j: logger automatico
 * - @RequiredArgsConstructor: inyeccion de dependencias por constructor
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentClient paymentClient;

    /**
     * Crea una orden y procesa el pago via payment-service.
     *
     * @CircuitBreaker: intercepta esta llamada con Resilience4j
     * - name: identificador del CB (debe coincidir con application.yml)
     * - fallbackMethod: metodo a ejecutar cuando el CB esta OPEN o hay error
     *
     * @param request datos de la orden
     * @return respuesta con los datos de la orden y resultado del pago
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "createOrderFallback")
    public OrderResponse createOrder(CreateOrderRequest request) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Creando orden {} para cliente: {}", orderId, request.customerName());

        // 1. Guardar la orden con estado CREATED
        CustomerOrder order = CustomerOrder.builder()
                .orderId(orderId)
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .productName(request.productName())
                .amount(request.amount())
                .currency(request.currency())
                .status(OrderStatus.CREATED)
                .statusMessage("Orden creada, procesando pago...")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        log.info("Orden {} guardada en BD con estado CREATED", orderId);

        // 2. Llamar al payment-service via Feign (comunicacion HTTP real)
        //    Esta llamada es la que protege el Circuit Breaker
        log.info("Enviando solicitud de pago al payment-service para orden {}", orderId);
        PaymentResponse paymentResponse = paymentClient.processPayment(
                new PaymentRequest(orderId, request.amount(), request.currency(), request.customerEmail())
        );
        log.info("Respuesta del payment-service: {} - {}", paymentResponse.status(), paymentResponse.message());

        // 3. Actualizar la orden con el resultado del pago
        order.setStatus(OrderStatus.PAID);
        order.setPaymentId(paymentResponse.paymentId());
        order.setStatusMessage("Pago confirmado: " + paymentResponse.message());
        order.setUpdatedAt(LocalDateTime.now());
        orderRepository.save(order);
        log.info("Orden {} actualizada a PAID con paymentId: {}", orderId, paymentResponse.paymentId());

        return toResponse(order);
    }

    /**
     * Metodo fallback del Circuit Breaker.
     *
     * Se ejecuta cuando:
     * 1. El circuito esta OPEN -> fallo rapido sin intentar la llamada HTTP
     * 2. La llamada al payment-service lanza excepcion (timeout, 500, conexion rechazada)
     *
     * IMPORTANTE: debe tener la MISMA firma que el metodo original + Throwable al final.
     *
     * Estrategia: guardar la orden como PAYMENT_PENDING para reprocesar despues.
     */
    public OrderResponse createOrderFallback(CreateOrderRequest request, Throwable throwable) {
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.warn("CIRCUIT BREAKER ACTIVADO - Fallback para orden {} - Causa: {}",
                orderId, throwable.getMessage());

        // Guardar la orden con estado PAYMENT_PENDING
        CustomerOrder order = CustomerOrder.builder()
                .orderId(orderId)
                .customerName(request.customerName())
                .customerEmail(request.customerEmail())
                .productName(request.productName())
                .amount(request.amount())
                .currency(request.currency())
                .status(OrderStatus.PAYMENT_PENDING)
                .statusMessage("El servicio de pagos no esta disponible. "
                        + "El pago sera procesado cuando se restablezca. "
                        + "Causa: " + throwable.getMessage())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        orderRepository.save(order);
        log.warn("Orden {} guardada como PAYMENT_PENDING", orderId);

        return toResponse(order);
    }

    /**
     * Lista todas las ordenes almacenadas en H2.
     */
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Convierte una entidad CustomerOrder a su DTO de respuesta.
     */
    private OrderResponse toResponse(CustomerOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getOrderId(),
                order.getCustomerName(),
                order.getCustomerEmail(),
                order.getProductName(),
                order.getAmount(),
                order.getCurrency(),
                order.getStatus().name(),
                order.getPaymentId(),
                order.getStatusMessage(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
