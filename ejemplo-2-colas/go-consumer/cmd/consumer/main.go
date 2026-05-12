// ============================================================
// Servicio Go - Consumidor de Pagos Idempotente
// ============================================================
//
// Punto de entrada del microservicio. Se encarga de:
//
// 1. Leer la configuracion desde variables de entorno.
// 2. Crear el productor Kafka (para enviar respuestas).
// 3. Crear el consumer group (para recibir solicitudes).
// 4. Iniciar el handler que procesa los mensajes.
// 5. Esperar la señal de terminacion (Ctrl+C o docker stop).
//
// Estructura del proyecto:
//   cmd/consumer/main.go        -> Punto de entrada (este archivo)
//   internal/model/payment.go   -> Estructuras de datos (Event, Response)
//   internal/store/             -> Almacen de claves de idempotencia
//   internal/handler/           -> Logica de procesamiento de mensajes
//   internal/kafka/             -> Configuracion y clientes de Kafka
//
// ============================================================
package main

import (
	"context"
	"log"
	"os"
	"os/signal"
	"syscall"
	"time"

	"go-consumer/internal/handler"
	"go-consumer/internal/kafka"
	"go-consumer/internal/store"
)

func main() {
	log.Println("[MAIN] Iniciando servicio Go - Consumidor de pagos idempotente")

	// ---- Leer configuracion desde variables de entorno ----
	brokers := os.Getenv("KAFKA_BROKERS")
	if brokers == "" {
		brokers = "localhost:9092"
	}
	log.Printf("[MAIN] Broker de Kafka configurado: %s", brokers)

	// Configuracion de Kafka
	kafkaConfig := kafka.Config{
		Brokers:       []string{brokers},
		GroupID:       "go-payment-consumer",
		MaxRetries:    30,
		RetryInterval: 2 * time.Second,
	}

	// ---- Crear el productor Kafka ----
	// Se usa para enviar respuestas al topic "payment-responses"
	producer, err := kafka.NewProducer(kafkaConfig)
	if err != nil {
		log.Fatalf("[MAIN] No se pudo crear el productor Kafka: %v", err)
	}
	defer producer.Close()

	// ---- Crear el consumer group ----
	// Permite consumir mensajes del topic "payment-requests"
	group, err := kafka.NewConsumerGroup(kafkaConfig)
	if err != nil {
		log.Fatalf("[MAIN] No se pudo crear el consumer group: %v", err)
	}
	defer group.Close()

	// ---- Crear el almacen de idempotencia ----
	// Registra las claves de transacciones ya procesadas
	idempotencyStore := store.New()

	// ---- Crear el handler de mensajes ----
	paymentHandler := &handler.PaymentHandler{
		Producer:      producer,
		Store:         idempotencyStore,
		ResponseTopic: "payment-responses",
	}

	// ---- Contexto cancelable para shutdown graceful ----
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	// ---- Capturar señales del sistema operativo ----
	// SIGINT = Ctrl+C, SIGTERM = docker stop
	sigchan := make(chan os.Signal, 1)
	signal.Notify(sigchan, syscall.SIGINT, syscall.SIGTERM)

	// ---- Iniciar el consumo de mensajes en un goroutine ----
	go func() {
		for {
			// Consume inicia el procesamiento de mensajes del topic.
			// Si ocurre un rebalanceo, se reconecta automaticamente.
			if err := group.Consume(ctx, []string{"payment-requests"}, paymentHandler); err != nil {
				log.Printf("[MAIN] Error en consumer group: %v", err)
			}
			// Si el contexto fue cancelado, salir del loop
			if ctx.Err() != nil {
				return
			}
		}
	}()

	log.Println("[MAIN] Escuchando mensajes en topic 'payment-requests'...")
	log.Println("[MAIN] Presiona Ctrl+C para detener el servicio")

	// ---- Esperar señal de terminacion ----
	<-sigchan
	log.Println("[MAIN] Senal de terminacion recibida. Cerrando conexiones...")
	cancel()
	log.Println("[MAIN] Servicio Go finalizado correctamente")
}
