package com.eia.circuitbreaker.service;

import com.eia.circuitbreaker.model.PaymentRequest;
import com.eia.circuitbreaker.model.PaymentResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Random;

/**
 * Servicio de pagos protegido con el patron Circuit Breaker.
 *
 * Utiliza la anotacion @CircuitBreaker de Resilience4j para envolver
 * la llamada al servicio externo de pagos. Si el servicio falla repetidamente,
 * el circuito se abre y las llamadas subsiguientes ejecutan el metodo fallback
 * en lugar de intentar la llamada real.
 *
 * Configuracion del Circuit Breaker (ver application.yml):
 * - slidingWindowSize: 10 (evalua las ultimas 10 llamadas)
 * - failureRateThreshold: 50% (abre si la mitad fallan)
 * - waitDurationInOpenState: 30s (espera 30s antes de probar recuperacion)
 * - permittedNumberOfCallsInHalfOpenState: 3 (permite 3 llamadas de prueba)
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final Random random = new Random();

    /**
     * Procesa un pago simulando una llamada a un servicio externo.
     *
     * La anotacion @CircuitBreaker intercepta esta llamada y:
     * - Estado CLOSED: permite la llamada y contabiliza fallos
     * - Estado OPEN: ejecuta directamente el fallback sin intentar la llamada
     * - Estado HALF-OPEN: permite un numero limitado de llamadas de prueba
     *
     * @param request datos del pago a procesar
     * @return respuesta del procesamiento del pago
     */
    @CircuitBreaker(name = "paymentService", fallbackMethod = "paymentFallback")
    public PaymentResponse processPayment(PaymentRequest request) {
        log.info("Procesando pago para orden: {} - Monto: {} {}",
                request.getOrderId(), request.getAmount(), request.getCurrency());

        // Simulacion: el servicio externo falla aleatoriamente (40% de probabilidad)
        // En produccion, aqui iria la llamada real al servicio de pagos externo
        simulateExternalServiceCall();

        log.info("Pago procesado exitosamente para orden: {}", request.getOrderId());
        return new PaymentResponse(
                request.getOrderId(),
                "APPROVED",
                "Pago procesado exitosamente por $" + request.getAmount() + " " + request.getCurrency()
        );
    }

    /**
     * Metodo fallback que se ejecuta cuando:
     * 1. El circuito esta OPEN (fallo rapido, sin intentar la llamada)
     * 2. La llamada real lanza una excepcion
     *
     * Este metodo debe tener la MISMA firma que el metodo original,
     * mas un parametro Throwable al final para recibir la excepcion.
     *
     * Estrategias comunes de fallback:
     * - Devolver datos del cache
     * - Devolver valores por defecto
     * - Encolar la operacion para procesamiento posterior
     * - Redirigir a un servicio alternativo
     *
     * @param request datos originales del pago
     * @param throwable excepcion que causo el fallo
     * @return respuesta alternativa indicando que el pago quedo pendiente
     */
    public PaymentResponse paymentFallback(PaymentRequest request, Throwable throwable) {
        log.warn("CIRCUIT BREAKER ACTIVADO - Fallback para orden: {} - Causa: {}",
                request.getOrderId(), throwable.getMessage());

        return new PaymentResponse(
                request.getOrderId(),
                "PENDING",
                "El servicio de pagos no esta disponible en este momento. "
                + "Su pago ha sido registrado y sera procesado automaticamente "
                + "cuando el servicio se restablezca. Causa: " + throwable.getMessage()
        );
    }

    /**
     * Simula el comportamiento de un servicio externo inestable.
     * En un entorno real, aqui se haria la llamada HTTP al servicio de pagos.
     *
     * Falla con un 40% de probabilidad para demostrar como el Circuit Breaker
     * detecta los fallos y abre el circuito.
     */
    private void simulateExternalServiceCall() {
        // Simular latencia del servicio externo (100-500ms)
        try {
            Thread.sleep(100 + random.nextInt(400));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Simular fallo del servicio externo (40% de probabilidad)
        if (random.nextInt(100) < 40) {
            throw new RuntimeException("Error de conexion con el servicio externo de pagos (timeout)");
        }
    }
}
