package com.eia.order.dto;

/**
 * DTO (Record) para recibir solicitudes de creacion de orden desde el frontend.
 *
 * Java Records son inmutables y generan automaticamente constructor,
 * getters (customerName(), amount(), etc.), toString, equals, hashCode.
 */
public record CreateOrderRequest(
        /** Nombre del cliente */
        String customerName,
        /** Email del cliente */
        String customerEmail,
        /** Producto o servicio */
        String productName,
        /** Monto a pagar */
        double amount,
        /** Moneda (USD, COP, EUR) */
        String currency
) {}
