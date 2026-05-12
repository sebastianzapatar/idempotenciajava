package com.eia.producer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Clase principal del servicio Java Productor.
 *
 * Este microservicio:
 * 1. Expone un endpoint REST para recibir solicitudes de pago.
 * 2. Publica el pago en el topic Kafka "payment-requests".
 * 3. Espera la respuesta en el topic "payment-responses".
 * 4. Si no recibe respuesta en 5 segundos, REINTENTA enviar
 *    el mismo mensaje con la MISMA clave de idempotencia.
 * 5. El servicio Go consumidor es quien garantiza que el pago
 *    solo se procese UNA vez gracias a la idempotencia.
 *
 * @EnableAsync permite ejecutar operaciones asíncronas (retries con timeout).
 */
@SpringBootApplication
@EnableAsync
public class ProducerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProducerApplication.class, args);
    }
}
