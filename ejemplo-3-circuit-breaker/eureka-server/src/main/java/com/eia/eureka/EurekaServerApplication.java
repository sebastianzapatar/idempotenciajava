package com.eia.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Servidor Eureka para descubrimiento de servicios.
 *
 * Eureka es un componente de Spring Cloud Netflix que actua como
 * registro central donde los microservicios se registran al iniciar
 * y consultan para encontrar a otros servicios.
 *
 * En esta arquitectura:
 * - order-service se registra y busca a payment-service via Eureka
 * - payment-service se registra para ser descubierto
 *
 * Dashboard disponible en: http://localhost:8761
 *
 * Universidad EIA - Ingenieria de Sistemas y Computacion - 2026
 */
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
