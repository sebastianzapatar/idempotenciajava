package com.eia.idempotencia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación Spring Boot.
 *
 * Esta aplicación demuestra el concepto de IDEMPOTENCIA en un sistema de compras.
 *
 * Escenario:
 * Un cliente tiene varios productos y puede hacer varias veces la solicitud
 * de compra (por error de red, doble clic, etc.), pero el sistema solo debe
 * procesar UNA sola vez gracias al uso de un Idempotency-Key.
 *
 * Tecnologías usadas:
 * - Spring Boot 3.1.5 (Framework web + REST)
 * - H2 Database (Base de datos en memoria para el ejemplo)
 * - Spring Data JPA (Acceso a datos simplificado)
 *
 * Estructura del proyecto:
 * - model/      → Entidades JPA (PurchaseOrder)
 * - dto/        → Data Transfer Objects como records (PurchaseRequest, PurchaseResponse)
 * - dao/        → Data Access Objects / Repositorios JPA (PurchaseOrderDao)
 * - service/    → Lógica de negocio e idempotencia (PurchaseService)
 * - controller/ → Endpoints REST (PurchaseController)
 *
 * @author Universidad EIA - Ingeniería de Sistemas y Computación
 */
@SpringBootApplication
public class IdempotenciaApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdempotenciaApplication.class, args);
    }

}
