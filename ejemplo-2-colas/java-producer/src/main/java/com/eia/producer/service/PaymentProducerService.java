package com.eia.producer.service;

import com.eia.producer.dto.PaymentRequest;
import com.eia.producer.dto.PaymentResponse;
import com.eia.producer.model.PaymentEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Servicio que maneja la producción de mensajes a Kafka y la lógica de reintentos.
 *
 * FLUJO COMPLETO:
 * ===============
 * 1. El controller recibe un POST con un pago y un Idempotency-Key.
 * 2. Este servicio publica el evento en el topic "payment-requests" de Kafka.
 * 3. Espera la respuesta en el topic "payment-responses" durante un timeout.
 * 4. Si el timeout expira sin respuesta, REINTENTA enviando el mismo mensaje
 *    con la MISMA clave de idempotencia (para que el servicio Go no lo duplique).
 * 5. Devuelve la respuesta al controller cuando la recibe o agota los reintentos.
 *
 * IDEMPOTENCIA (lado productor):
 * - Se usa la misma Idempotency-Key en cada reintento.
 * - El servicio Go es responsable de verificar duplicados.
 * - Aquí solo nos aseguramos de no perder el mensaje.
 */
@Service
public class PaymentProducerService {

    private static final Logger log = LoggerFactory.getLogger(PaymentProducerService.class);

    // Template de Kafka para enviar mensajes
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Serializador/deserializador JSON
    private final ObjectMapper objectMapper;

    // Mapa de respuestas pendientes: idempotencyKey → CompletableFuture con la respuesta
    // Permite que el listener de Kafka "despierte" al hilo que está esperando la respuesta
    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();

    // Topic donde se publican las solicitudes de pago
    @Value("${app.kafka.topic-requests}")
    private String topicRequests;

    // Topic donde se reciben las respuestas del servicio Go
    @Value("${app.kafka.topic-responses}")
    private String topicResponses;

    // Tiempo máximo de espera por respuesta (en milisegundos)
    @Value("${app.timeout-ms}")
    private long timeoutMs;

    // Número máximo de reintentos
    @Value("${app.max-retries}")
    private int maxRetries;

    public PaymentProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Envía un pago a Kafka con reintentos automáticos y espera la respuesta.
     *
     * Algoritmo:
     * 1. Serializa el evento de pago a JSON.
     * 2. Registra un CompletableFuture para esperar la respuesta.
     * 3. Publica el mensaje en Kafka con la Idempotency-Key como clave del mensaje.
     * 4. Espera la respuesta durante el timeout configurado.
     * 5. Si no llega respuesta, reintenta (máximo N veces).
     * 6. En cada reintento, se envía el MISMO mensaje con la MISMA clave.
     *
     * @param idempotencyKey clave UUID única de esta transacción
     * @param request        datos del pago
     * @return PaymentResponse con el resultado
     */
    public PaymentResponse sendPaymentWithRetry(String idempotencyKey, PaymentRequest request) {
        // Construir el evento de pago
        PaymentEvent event = new PaymentEvent(
                idempotencyKey,
                request.clientName(),
                request.description(),
                request.amount(),
                System.currentTimeMillis()
        );

        // Intentar enviar con reintentos
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                log.info("[PRODUCER] Intento {}/{} - Enviando pago con key: {}", attempt, maxRetries, idempotencyKey);

                // Serializar el evento a JSON
                String eventJson = objectMapper.writeValueAsString(event);

                // Registrar un Future para esperar la respuesta
                CompletableFuture<String> responseFuture = new CompletableFuture<>();
                pendingResponses.put(idempotencyKey, responseFuture);

                // Publicar en Kafka: la KEY del mensaje es el Idempotency-Key
                // Esto garantiza que mensajes con la misma clave van a la misma partición
                kafkaTemplate.send(topicRequests, idempotencyKey, eventJson);
                log.info("[PRODUCER] Mensaje publicado en topic '{}' con key '{}'", topicRequests, idempotencyKey);

                // Esperar la respuesta con timeout
                String responseJson = responseFuture.get(timeoutMs, TimeUnit.MILLISECONDS);
                log.info("[PRODUCER] Respuesta recibida del servicio Go: {}", responseJson);

                // Limpiar el mapa de pendientes
                pendingResponses.remove(idempotencyKey);

                // Deserializar y devolver la respuesta
                Map<String, Object> responseMap = objectMapper.readValue(responseJson, Map.class);
                String status = (String) responseMap.getOrDefault("status", "UNKNOWN");
                String message = (String) responseMap.getOrDefault("message", "");
                boolean duplicate = "DUPLICATE".equals(status);

                return new PaymentResponse(
                        idempotencyKey,
                        status,
                        message,
                        true,
                        attempt
                );

            } catch (java.util.concurrent.TimeoutException e) {
                // TIMEOUT: No se recibio respuesta en el tiempo configurado
                log.warn("[PRODUCER] Timeout en intento {}/{} para key: {}. Reintentando...",
                        attempt, maxRetries, idempotencyKey);
                pendingResponses.remove(idempotencyKey);

                // Si es el último intento, devolver respuesta de timeout
                if (attempt == maxRetries) {
                    log.error("[PRODUCER] Se agotaron los {} reintentos para key: {}", maxRetries, idempotencyKey);
                    return new PaymentResponse(
                            idempotencyKey,
                            "TIMEOUT",
                            "No se recibió respuesta del servicio Go después de " + maxRetries + " intentos",
                            false,
                            attempt
                    );
                }

            } catch (Exception e) {
                log.error("[PRODUCER] Error inesperado en intento {}: {}", attempt, e.getMessage());
                pendingResponses.remove(idempotencyKey);
            }
        }

        // Fallback: no debería llegar aquí, pero por seguridad
        return new PaymentResponse(idempotencyKey, "ERROR", "Error desconocido", false, maxRetries);
    }

    /**
     * Listener de Kafka que escucha las respuestas del servicio Go.
     *
     * Cuando el servicio Go procesa un pago (o detecta un duplicado),
     * publica la respuesta en el topic "payment-responses".
     * Este listener la recibe y completa el CompletableFuture correspondiente,
     * "despertando" al hilo que estaba esperando en sendPaymentWithRetry().
     *
     * @param message el JSON de respuesta publicado por el servicio Go
     */
    @KafkaListener(topics = "${app.kafka.topic-responses}", groupId = "java-producer-group")
    public void listenResponses(String message) {
        try {
            log.info("[LISTENER] Respuesta recibida de Go: {}", message);

            // Extraer el idempotencyKey de la respuesta para saber a cuál solicitud pertenece
            Map<String, Object> responseMap = objectMapper.readValue(message, Map.class);
            String idempotencyKey = (String) responseMap.get("idempotency_key");

            // Buscar si hay un hilo esperando esta respuesta
            CompletableFuture<String> future = pendingResponses.get(idempotencyKey);
            if (future != null) {
                // Completar el Future, lo que desbloquea al hilo que espera
                future.complete(message);
            } else {
                log.warn("[LISTENER] Respuesta recibida para key '{}' pero nadie la esperaba (ya expiro el timeout)", idempotencyKey);
            }

        } catch (Exception e) {
            log.error("[LISTENER] Error procesando respuesta: {}", e.getMessage());
        }
    }
}
