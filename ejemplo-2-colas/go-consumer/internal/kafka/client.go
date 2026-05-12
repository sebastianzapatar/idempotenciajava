// Package kafka encapsula la configuracion y creacion de clientes Kafka.
//
// Proporciona funciones para crear productores y consumidores
// con la configuracion necesaria para el sistema de pagos.
package kafka

import (
	"log"
	"time"

	"github.com/IBM/sarama"
)

// Config contiene los parametros de configuracion de Kafka.
type Config struct {
	// Brokers es la lista de direcciones de los brokers Kafka.
	Brokers []string

	// GroupID es el identificador del consumer group.
	GroupID string

	// MaxRetries es el numero maximo de intentos de conexion al arrancar.
	MaxRetries int

	// RetryInterval es el tiempo entre reintentos de conexion.
	RetryInterval time.Duration
}

// NewSaramaConfig crea una configuracion base de Sarama
// compartida entre productores y consumidores.
func NewSaramaConfig() *sarama.Config {
	config := sarama.NewConfig()

	// Configuracion del consumidor
	// RoundRobin distribuye las particiones equitativamente entre los consumidores
	config.Consumer.Group.Rebalance.Strategy = sarama.NewBalanceStrategyRoundRobin()
	// Si no hay offset guardado, leer desde el inicio del topic
	config.Consumer.Offsets.Initial = sarama.OffsetOldest

	// Configuracion del productor
	// Return.Successes debe ser true para usar SyncProducer
	config.Producer.Return.Successes = true

	// Configuracion de red
	// Timeout largo para esperar a que Kafka arranque en Docker
	config.Net.DialTimeout = 30 * time.Second

	return config
}

// NewProducer crea un productor sincrono de Kafka.
// Reintenta la conexion hasta MaxRetries veces si Kafka no esta listo.
//
// Un productor sincrono bloquea hasta que Kafka confirma la recepcion
// del mensaje, lo cual es mas seguro para sistemas de pago.
func NewProducer(cfg Config) (sarama.SyncProducer, error) {
	saramaConfig := NewSaramaConfig()

	var producer sarama.SyncProducer
	var err error

	for i := 1; i <= cfg.MaxRetries; i++ {
		producer, err = sarama.NewSyncProducer(cfg.Brokers, saramaConfig)
		if err == nil {
			log.Printf("[KAFKA] Productor conectado exitosamente a %v", cfg.Brokers)
			return producer, nil
		}
		log.Printf("[KAFKA] Esperando conexion a Kafka (intento %d/%d): %v",
			i, cfg.MaxRetries, err)
		time.Sleep(cfg.RetryInterval)
	}

	return nil, err
}

// NewConsumerGroup crea un consumer group de Kafka.
// Un consumer group permite que multiples instancias del servicio
// consuman mensajes de forma balanceada (cada mensaje va a una sola instancia).
func NewConsumerGroup(cfg Config) (sarama.ConsumerGroup, error) {
	saramaConfig := NewSaramaConfig()

	group, err := sarama.NewConsumerGroup(cfg.Brokers, cfg.GroupID, saramaConfig)
	if err != nil {
		return nil, err
	}

	log.Printf("[KAFKA] Consumer group '%s' creado exitosamente", cfg.GroupID)
	return group, nil
}
