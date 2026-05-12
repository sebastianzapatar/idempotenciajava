// Package handler contiene la logica de procesamiento de mensajes de Kafka.
//
// Implementa la interfaz sarama.ConsumerGroupHandler, que define
// los metodos que Kafka invoca al asignar, procesar y liberar particiones.
package handler

import (
	"encoding/json"
	"fmt"
	"log"
	"time"

	"github.com/IBM/sarama"

	"go-consumer/internal/model"
	"go-consumer/internal/store"
)

// PaymentHandler procesa los mensajes de pago recibidos de Kafka.
// Implementa sarama.ConsumerGroupHandler.
//
// Responsabilidades:
//   - Deserializar el evento de pago desde JSON.
//   - Verificar la idempotencia usando el IdempotencyStore.
//   - Procesar el pago si es nuevo.
//   - Publicar la respuesta en el topic "payment-responses".
type PaymentHandler struct {
	// Producer es el productor de Kafka para enviar respuestas.
	Producer sarama.SyncProducer

	// Store es el almacen de claves de idempotencia.
	Store *store.IdempotencyStore

	// ResponseTopic es el nombre del topic donde se publican las respuestas.
	ResponseTopic string
}

// Setup se ejecuta al inicio de cada sesion del consumer group.
// Kafka invoca este metodo cuando se asignan particiones al consumidor.
func (h *PaymentHandler) Setup(_ sarama.ConsumerGroupSession) error {
	log.Println("[HANDLER] Sesion del consumer group iniciada")
	return nil
}

// Cleanup se ejecuta al finalizar cada sesion del consumer group.
// Kafka invoca este metodo cuando se revocan las particiones del consumidor.
func (h *PaymentHandler) Cleanup(_ sarama.ConsumerGroupSession) error {
	log.Println("[HANDLER] Sesion del consumer group finalizada")
	return nil
}

// ConsumeClaim procesa los mensajes de una particion especifica.
//
// Este es el metodo donde se implementa la IDEMPOTENCIA:
// 1. Deserializa el evento de pago.
// 2. Verifica si la clave ya fue procesada (IdempotencyStore).
// 3. Si es duplicado: responde con status "DUPLICATE".
// 4. Si es nuevo: procesa el pago y responde con status "PROCESSED".
// 5. Publica la respuesta en el topic de respuestas.
// 6. Marca el mensaje como procesado (commit del offset).
func (h *PaymentHandler) ConsumeClaim(session sarama.ConsumerGroupSession, claim sarama.ConsumerGroupClaim) error {
	for message := range claim.Messages() {
		h.processMessage(session, message)
	}
	return nil
}

// processMessage maneja un mensaje individual de Kafka.
// Separado del loop principal para mantener el codigo limpio.
func (h *PaymentHandler) processMessage(session sarama.ConsumerGroupSession, message *sarama.ConsumerMessage) {
	log.Printf("[HANDLER] Mensaje recibido - Topic: %s, Partition: %d, Offset: %d, Key: %s",
		message.Topic, message.Partition, message.Offset, string(message.Key))

	// Paso 1: Deserializar el evento de pago desde JSON
	var event model.PaymentEvent
	if err := json.Unmarshal(message.Value, &event); err != nil {
		log.Printf("[HANDLER] Error deserializando mensaje: %v", err)
		session.MarkMessage(message, "")
		return
	}

	// Paso 2: Verificar idempotencia
	// CheckAndStore retorna true si la clave YA existia (duplicado)
	isDuplicate := h.Store.CheckAndStore(event.IdempotencyKey)

	// Paso 3: Construir la respuesta segun si es duplicado o nuevo
	var response model.PaymentResponse

	if isDuplicate {
		// DUPLICADO: la clave ya fue procesada antes.
		// NO se procesa el pago de nuevo.
		log.Printf("[HANDLER] DUPLICADO detectado - Key: %s (total claves: %d)",
			event.IdempotencyKey, h.Store.Count())

		response = model.PaymentResponse{
			IdempotencyKey: event.IdempotencyKey,
			Status:         "DUPLICATE",
			Message: fmt.Sprintf(
				"El pago con key '%s' ya fue procesado anteriormente. No se duplico el cobro.",
				event.IdempotencyKey),
			ProcessedAt: time.Now().Format(time.RFC3339),
		}
	} else {
		// NUEVO: primera vez que vemos esta clave.
		// Procesamos el pago (aqui iria la logica de negocio real).
		log.Printf("[HANDLER] Procesando pago nuevo - Key: %s, Cliente: %s, Monto: $%.2f",
			event.IdempotencyKey, event.ClientName, event.Amount)

		// Simular procesamiento de pago (500ms)
		time.Sleep(500 * time.Millisecond)

		log.Printf("[HANDLER] Pago procesado exitosamente - Key: %s (total claves: %d)",
			event.IdempotencyKey, h.Store.Count())

		response = model.PaymentResponse{
			IdempotencyKey: event.IdempotencyKey,
			Status:         "PROCESSED",
			Message: fmt.Sprintf(
				"Pago de $%.2f para '%s' procesado correctamente.",
				event.Amount, event.ClientName),
			ProcessedAt: time.Now().Format(time.RFC3339),
		}
	}

	// Paso 4: Publicar la respuesta en Kafka
	h.sendResponse(event.IdempotencyKey, response)

	// Paso 5: Marcar el mensaje como procesado (commit del offset)
	// Esto le dice a Kafka que este mensaje ya no necesita ser re-entregado.
	session.MarkMessage(message, "")
}

// sendResponse publica la respuesta en el topic de respuestas de Kafka.
func (h *PaymentHandler) sendResponse(key string, response model.PaymentResponse) {
	responseJSON, err := json.Marshal(response)
	if err != nil {
		log.Printf("[HANDLER] Error serializando respuesta: %v", err)
		return
	}

	_, _, err = h.Producer.SendMessage(&sarama.ProducerMessage{
		Topic: h.ResponseTopic,
		Key:   sarama.StringEncoder(key),
		Value: sarama.ByteEncoder(responseJSON),
	})

	if err != nil {
		log.Printf("[HANDLER] Error enviando respuesta a Kafka: %v", err)
	} else {
		log.Printf("[HANDLER] Respuesta enviada - Topic: %s, Key: %s, Status: %s",
			h.ResponseTopic, key, response.Status)
	}
}
