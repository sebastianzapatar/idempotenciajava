# 🔁 Ejemplo 2: Idempotencia con Kafka (Java + Go)

> **Universidad EIA** · Ingeniería de Sistemas y Computación · 2026

Ejemplo de **idempotencia en un sistema de colas de mensajes** donde dos microservicios se comunican a través de **Apache Kafka**.

---

## 📖 Escenario

Un servicio **Java** recibe solicitudes de pago por HTTP y las publica en Kafka. Un servicio **Go** consume esos mensajes, procesa los pagos y devuelve la respuesta por Kafka.

**Problema:** ¿Qué pasa si la red falla y Java no recibe la respuesta del servicio Go? Java **reintenta** enviando el mismo mensaje, pero Go ya lo procesó. Sin idempotencia, el pago se cobraría dos veces.

**Solución:** Cada mensaje lleva un `Idempotency-Key` (UUID). El servicio Go verifica si esa clave ya fue procesada antes. Si ya existe, **ignora el duplicado** y devuelve el resultado original.

---

## 🏗️ Arquitectura

```
                    ┌─────────────────┐
   HTTP POST        │                 │     payment-requests
   + Idempotency    │   Java          │  ─────────────────►  ┌─────────┐
   Key              │   Producer      │                      │         │
────────────────►   │   (Spring Boot) │                      │  KAFKA  │
                    │                 │  ◄─────────────────  │         │
                    │   Espera resp.  │     payment-responses └─────────┘
                    │   Timeout: 5s   │                          │
                    │   Retries: 3    │                          │
                    └─────────────────┘                          │
                                                                 │
                                                                 ▼
                                                        ┌─────────────────┐
                                                        │                 │
                                                        │   Go            │
                                                        │   Consumer      │
                                                        │                 │
                                                        │   Verifica Key  │
                                                        │   en sync.Map   │
                                                        │                 │
                                                        │   ¿Ya existe?   │
                                                        │   SÍ → IGNORA   │
                                                        │   NO → PROCESA  │
                                                        └─────────────────┘
```

---

## 🛠️ Tecnologías

| Componente       | Tecnología                | Versión |
|------------------|---------------------------|---------|
| **Productor**    | Spring Boot + Spring Kafka | 3.1.5   |
| **Lenguaje**     | Java                      | 21 LTS  |
| **Consumidor**   | Go + Sarama               | 1.22    |
| **Mensajería**   | Apache Kafka              | 7.5.0   |
| **Coordinación** | Zookeeper                 | 7.5.0   |
| **Contenedores** | Docker Compose            | —       |

---

## 📂 Estructura del Proyecto

```
ejemplo-2-colas/
├── compose.yml                          # Kafka + Zookeeper + Java + Go
│
├── java-producer/                       # Servicio Java (productor)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/eia/producer/
│       ├── ProducerApplication.java     # Main class
│       ├── model/
│       │   └── PaymentEvent.java        # Evento que viaja por Kafka
│       ├── dto/
│       │   ├── PaymentRequest.java      # DTO entrada (record)
│       │   └── PaymentResponse.java     # DTO salida (record)
│       ├── service/
│       │   └── PaymentProducerService.java  # Lógica de reintentos + Kafka
│       └── controller/
│           └── PaymentController.java   # Endpoint REST
│
└── go-consumer/                         # Servicio Go (consumidor)
    ├── Dockerfile
    ├── go.mod
    └── main.go                          # Consumidor idempotente
```

---

## 🚀 Cómo Ejecutar

```bash
cd ejemplo-2-colas

# Construir y levantar todos los servicios
docker compose up --build
```

| Servicio          | Puerto |
|-------------------|--------|
| **Java API**      | http://localhost:8081 |
| **Kafka**         | localhost:9092 |
| **Zookeeper**     | localhost:2181 |

---

## 🧪 Probar la Idempotencia

### 1. Enviar un pago (primera vez → PROCESSED)
```bash
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pago-001" \
  -d '{"clientName":"Juan Pérez","description":"Compra de laptop","amount":1500.00}'
```

### 2. Reenviar CON LA MISMA KEY (→ DUPLICATE)
```bash
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pago-001" \
  -d '{"clientName":"Juan Pérez","description":"Compra de laptop","amount":1500.00}'
```

### 3. Enviar un pago DIFERENTE (nueva key → PROCESSED)
```bash
curl -X POST http://localhost:8081/api/payments \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pago-002" \
  -d '{"clientName":"María López","description":"Compra de monitor","amount":800.00}'
```

---

## 🔑 ¿Cómo funciona la Idempotencia?

### En el servicio Java (Productor):
1. Recibe un `POST` con un `Idempotency-Key` en el header.
2. Publica el mensaje en Kafka con esa key como identificador del mensaje.
3. Espera la respuesta del servicio Go durante **5 segundos** (timeout configurable).
4. Si **no recibe respuesta** → reintenta enviando el **mismo mensaje con la misma key**.
5. Máximo **3 reintentos** antes de devolver `TIMEOUT`.

### En el servicio Go (Consumidor):
1. Recibe el mensaje de Kafka.
2. Extrae el `idempotencyKey` del evento.
3. Consulta un `sync.Map` (almacén thread-safe de claves procesadas).
4. Si la clave **ya existe** → responde con `status: "DUPLICATE"` sin procesar.
5. Si la clave **no existe** → procesa el pago, guarda la clave, responde `status: "PROCESSED"`.

### Diagrama de secuencia:
```
Java Producer              Kafka              Go Consumer
    │                        │                      │
    │  ── Publica pago ──►   │                      │
    │                        │  ── Entrega ──────►  │
    │                        │                      │ ¿Key existe?
    │                        │                      │  NO → Procesa
    │                        │                      │  Guarda key
    │                        │  ◄── Respuesta ────  │
    │  ◄── Respuesta ──────  │                      │
    │   (PROCESSED)          │                      │
    │                        │                      │
    │  ── Reintento ──────►  │  (mismo key)         │
    │                        │  ── Entrega ──────►  │
    │                        │                      │ ¿Key existe?
    │                        │                      │  SÍ → IGNORA
    │                        │  ◄── Respuesta ────  │
    │  ◄── Respuesta ──────  │                      │
    │   (DUPLICATE)          │                      │
```

---

## 📚 Conceptos Clave

| Concepto | Descripción |
|----------|-------------|
| **Idempotency-Key** | UUID único generado por el cliente para cada transacción |
| **At-least-once delivery** | Kafka garantiza que un mensaje se entrega al menos una vez, pero puede entregarlo más de una vez |
| **sync.Map** | Estructura de Go thread-safe usada como almacén de claves procesadas (en producción sería Redis o una DB) |
| **CompletableFuture** | Mecanismo de Java para esperar respuestas asíncronas con timeout |
| **Consumer Group** | Permite que múltiples instancias del consumidor Go procesen mensajes de forma balanceada |

---

## 📚 Referencias

- [FreeCodeCamp — "What is Idempotence?"](https://www.freecodecamp.org/news/idempotence-explained/)
- [NetMentor — "La idempotencia en el Desarrollo de Software"](https://www.netmentor.es/entrada/idempotencia-desarrollo-software)
- [Apache Kafka Documentation](https://kafka.apache.org/documentation/)
- [Sarama — Go client for Kafka](https://github.com/IBM/sarama)

---

## 👨‍🎓 Autor

Proyecto académico para la **Universidad EIA** — Ingeniería de Sistemas y Computación.
