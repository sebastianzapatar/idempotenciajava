// Package model define las estructuras de datos que viajan a través de Kafka
// entre el servicio Java (productor) y el servicio Go (consumidor).
package model

// PaymentEvent representa el evento de pago recibido desde Kafka.
// Esta estructura debe coincidir con la clase PaymentEvent.java del servicio Java.
// Los campos JSON usan camelCase para mantener compatibilidad con Java/Jackson.
type PaymentEvent struct {
	// IdempotencyKey es la clave UUID unica que identifica esta transaccion.
	// Es la pieza central del patron de idempotencia.
	IdempotencyKey string `json:"idempotencyKey"`

	// ClientName es el nombre del cliente que realiza el pago.
	ClientName string `json:"clientName"`

	// Description es la descripcion del concepto de pago.
	Description string `json:"description"`

	// Amount es el monto del pago en USD.
	Amount float64 `json:"amount"`

	// Timestamp es la marca de tiempo en la que se genero el evento (epoch millis).
	Timestamp int64 `json:"timestamp"`
}

// PaymentResponse representa la respuesta que se envia de vuelta al servicio Java
// a traves del topic Kafka "payment-responses".
// Los campos JSON usan snake_case porque asi los espera el servicio Java.
type PaymentResponse struct {
	// IdempotencyKey es la misma clave que se recibio en el evento.
	IdempotencyKey string `json:"idempotency_key"`

	// Status indica el resultado: "PROCESSED" (nuevo) o "DUPLICATE" (ya existia).
	Status string `json:"status"`

	// Message es un mensaje descriptivo del resultado.
	Message string `json:"message"`

	// ProcessedAt es la fecha/hora en formato RFC3339 del procesamiento.
	ProcessedAt string `json:"processed_at"`
}
