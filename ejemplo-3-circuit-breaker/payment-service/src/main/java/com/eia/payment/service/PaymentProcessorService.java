package com.eia.payment.service;

import com.eia.payment.dto.PaymentRequest;
import com.eia.payment.dto.PaymentResponse;
import com.eia.payment.entity.Payment;
import com.eia.payment.entity.Payment.PaymentStatus;
import com.eia.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio que procesa pagos y los almacena en la base de datos H2.
 *
 * Incluye un mecanismo de toggle (AtomicBoolean) para simular
 * que el servicio esta caido. Cuando esta desactivado, lanza una
 * excepcion que el order-service detectara como fallo, activando
 * eventualmente el Circuit Breaker.
 *
 * Lombok:
 * - @Slf4j: genera el logger automaticamente
 * - @RequiredArgsConstructor: inyecta dependencias via constructor (campos final)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentProcessorService {

    private final PaymentRepository paymentRepository;

    /**
     * Flag atomico para simular disponibilidad del servicio.
     * Cuando es false, el servicio rechaza todas las peticiones,
     * lo cual dispara el Circuit Breaker en el order-service.
     */
    private final AtomicBoolean serviceAvailable = new AtomicBoolean(true);

    /**
     * Procesa un pago: valida, persiste en H2 y retorna la respuesta.
     *
     * Si el servicio esta desactivado (toggle off), lanza RuntimeException
     * para que el Circuit Breaker del order-service detecte el fallo.
     *
     * @param request datos del pago a procesar
     * @return respuesta con los datos del pago procesado
     * @throws RuntimeException si el servicio esta desactivado
     */
    public PaymentResponse processPayment(PaymentRequest request) {
        // Verificar si el servicio esta "disponible"
        if (!serviceAvailable.get()) {
            log.error("SERVICIO DESACTIVADO - Rechazando pago para orden: {}", request.orderId());
            throw new RuntimeException("Payment service no disponible - servicio desactivado para simulacion");
        }

        log.info("Procesando pago para orden: {} - Monto: {} {}",
                request.orderId(), request.amount(), request.currency());

        // Simular un pequeno delay de procesamiento (como un servicio real)
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Generar ID unico para el pago
        String paymentId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Crear y guardar la entidad Payment en H2
        Payment payment = Payment.builder()
                .paymentId(paymentId)
                .orderId(request.orderId())
                .amount(request.amount())
                .currency(request.currency())
                .customerEmail(request.customerEmail())
                .status(PaymentStatus.PROCESSED)
                .message("Pago procesado exitosamente por $" + request.amount() + " " + request.currency())
                .processedAt(LocalDateTime.now())
                .build();

        paymentRepository.save(payment);
        log.info("Pago guardado en BD - ID: {} para orden: {}", paymentId, request.orderId());

        // Convertir entidad a DTO de respuesta
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getMessage(),
                payment.getProcessedAt()
        );
    }

    /**
     * Alterna la disponibilidad del servicio (toggle).
     * Permite simular que el servicio esta caido sin apagar el contenedor.
     *
     * @return nuevo estado de disponibilidad
     */
    public boolean toggleAvailability() {
        boolean newState = !serviceAvailable.get();
        serviceAvailable.set(newState);
        log.warn("Disponibilidad del servicio cambiada a: {}", newState ? "ACTIVO" : "DESACTIVADO");
        return newState;
    }

    /** @return true si el servicio esta disponible */
    public boolean isAvailable() {
        return serviceAvailable.get();
    }

    /** @return lista de todos los pagos registrados en H2 */
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll().stream()
                .map(p -> new PaymentResponse(
                        p.getPaymentId(),
                        p.getOrderId(),
                        p.getAmount(),
                        p.getCurrency(),
                        p.getStatus().name(),
                        p.getMessage(),
                        p.getProcessedAt()
                ))
                .toList();
    }
}
