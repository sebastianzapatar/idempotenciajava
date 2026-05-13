package com.eia.circuitbreaker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicacion principal del ejemplo de Circuit Breaker.
 * Demuestra el uso del patron Circuit Breaker con Spring Boot y Resilience4j.
 *
 * Universidad EIA - Ingenieria de Sistemas y Computacion - 2026
 */
@SpringBootApplication
public class CircuitBreakerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CircuitBreakerApplication.class, args);
    }
}
