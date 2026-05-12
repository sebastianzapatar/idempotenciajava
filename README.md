# 🔁 Idempotencia en el Desarrollo de Software

> **Universidad EIA** · Ingeniería de Sistemas y Computación · 2026

Proyecto educativo que demuestra el concepto de **idempotencia** aplicado a un sistema de compras en línea. Un cliente puede enviar múltiples veces la misma solicitud de compra (por error de red, doble clic, timeout, etc.), pero el sistema **solo procesa la transacción una vez** gracias al uso de un `Idempotency-Key`.

---

## 📖 ¿Qué es la Idempotencia?

La idempotencia es una propiedad de una operación que garantiza que, **sin importar cuántas veces se ejecute**, el resultado final será exactamente el mismo que si se hubiera ejecutado una sola vez.

```
f(f(x)) = f(x)
```

En software: si un pago se procesa dos veces por un error de red, la segunda ejecución **no debe generar un segundo cobro**.

---

## 🏗️ Arquitectura del Proyecto

```
idempotencia/
├── 📄 compose.yml                  # Docker Compose (backend + frontend)
├── 📄 presentacion.html            # Presentación educativa (navegación con teclado ← →)
├── 📁 img/                         # Imágenes y diagramas de la presentación
│
├── 📁 backend/                     # API REST (Spring Boot + H2)
│   ├── 📄 Dockerfile
│   ├── 📄 pom.xml
│   └── 📁 src/main/java/com/eia/idempotencia/
│       ├── IdempotenciaApplication.java      # Clase principal
│       ├── 📁 model/
│       │   └── PurchaseOrder.java            # Entidad JPA (@Data con Lombok)
│       ├── 📁 dto/
│       │   ├── PurchaseRequest.java          # DTO de entrada (Java record)
│       │   └── PurchaseResponse.java         # DTO de salida (Java record)
│       ├── 📁 dao/
│       │   └── PurchaseOrderDao.java         # Repositorio JPA
│       ├── 📁 service/
│       │   └── PurchaseService.java          # Lógica de negocio + idempotencia
│       └── 📁 controller/
│           └── PurchaseController.java       # Endpoints REST
│
└── 📁 frontend/                    # UI (React + Vite)
    ├── 📄 Dockerfile
    ├── 📄 package.json
    └── 📁 src/
        ├── App.jsx                           # Componente principal
        └── index.css                         # Estilos (paleta EIA)
```

---

## 🛠️ Tecnologías

| Componente     | Tecnología                        | Versión |
|----------------|-----------------------------------|---------|
| **Backend**    | Spring Boot                       | 3.1.5   |
| **Lenguaje**   | Java                              | 21 LTS  |
| **Base de datos** | H2 (en memoria)               | —       |
| **ORM**        | Spring Data JPA (Hibernate)       | —       |
| **Utilidades** | Lombok                            | —       |
| **Frontend**   | React + Vite                      | 18+     |
| **Contenedores** | Docker + Docker Compose         | —       |

---

## 🚀 Cómo Ejecutar

### Opción 1: Con Docker Compose (recomendado)

```bash
# Clonar/abrir el proyecto
cd idempotencia

# Construir y levantar ambos servicios
docker compose up --build
```

| Servicio       | URL                                      |
|----------------|------------------------------------------|
| **Frontend**   | http://localhost:5173                     |
| **Backend API**| http://localhost:8080/api/purchases       |
| **H2 Console** | http://localhost:8080/h2-console          |

### Opción 2: Ejecución local (sin Docker)

**Backend:**
```bash
cd backend
./mvnw spring-boot:run
```

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

### Presentación:
Abrir `presentacion.html` directamente en cualquier navegador. Navegar con las teclas `←` `→`.

---

## 🔑 ¿Cómo funciona la Idempotencia en este proyecto?

### Flujo de la solicitud:

```
Cliente (React)                          Servidor (Spring Boot)
     │                                          │
     │  POST /api/purchases                     │
     │  Header: Idempotency-Key: <UUID>         │
     │  Body: {clientName, products, amount}     │
     │ ────────────────────────────────────────► │
     │                                          │
     │                         ┌────────────────┤
     │                         │ ¿Existe UUID   │
     │                         │ en la DB?      │
     │                         └───┬────────────┤
     │                             │            │
     │                        SÍ   │   NO       │
     │                             │            │
     │  ◄── 200 OK ───────────────┘            │
     │  (devuelve resultado original)           │
     │                                          │
     │  ◄── 201 CREATED ──────────────────────┘│
     │  (procesa y guarda nueva orden)          │
```

### Detalle paso a paso:

1. El **frontend** genera un UUID único (`crypto.randomUUID()`) como `Idempotency-Key`.
2. Envía la solicitud `POST` con ese UUID en el header `Idempotency-Key`.
3. El **servicio** (`PurchaseService`) busca en la base de datos si ya existe una orden con esa clave.
4. **Si existe** → Devuelve el resultado original con `HTTP 200` y `alreadyProcessed: true`.
5. **Si no existe** → Crea la orden, la guarda, y devuelve `HTTP 201` con `alreadyProcessed: false`.
6. El usuario puede hacer clic en "Comprar" múltiples veces con la misma Key: **solo se crea un registro**.

---

## 📡 API Endpoints

### `POST /api/purchases` — Crear una compra (idempotente)

**Headers:**
| Header            | Requerido | Descripción                           |
|-------------------|-----------|---------------------------------------|
| `Idempotency-Key` | ✅ Sí     | UUID único que identifica la transacción |
| `Content-Type`    | ✅ Sí     | `application/json`                    |

**Body:**
```json
{
  "clientName": "Juan Pérez",
  "productDetails": "Laptop, Ratón, Teclado",
  "totalAmount": 1500.50
}
```

**Respuesta (primera vez — 201 CREATED):**
```json
{
  "id": 1,
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "clientName": "Juan Pérez",
  "productDetails": "Laptop, Ratón, Teclado",
  "totalAmount": 1500.50,
  "status": "COMPLETED",
  "alreadyProcessed": false
}
```

**Respuesta (segunda vez con misma Key — 200 OK):**
```json
{
  "id": 1,
  "idempotencyKey": "550e8400-e29b-41d4-a716-446655440000",
  "clientName": "Juan Pérez",
  "productDetails": "Laptop, Ratón, Teclado",
  "totalAmount": 1500.50,
  "status": "COMPLETED",
  "alreadyProcessed": true
}
```

### `GET /api/purchases` — Listar todas las compras

```bash
curl http://localhost:8080/api/purchases
```

---

## 🧪 Probar la Idempotencia con cURL

```bash
# Primera solicitud — se crea la orden (201)
curl -X POST http://localhost:8080/api/purchases \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: mi-uuid-unico-123" \
  -d '{"clientName":"Juan","productDetails":"Laptop","totalAmount":1500}'

# Segunda solicitud CON LA MISMA KEY — no se duplica (200)
curl -X POST http://localhost:8080/api/purchases \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: mi-uuid-unico-123" \
  -d '{"clientName":"Juan","productDetails":"Laptop","totalAmount":1500}'

# Verificar que solo existe UN registro
curl http://localhost:8080/api/purchases
```

---

## 📂 Decisiones de Diseño

| Decisión | Justificación |
|----------|---------------|
| **Java Records para DTOs** | Inmutables, concisos y autogeneran `equals()`, `hashCode()`, `toString()`. Ideales para transferir datos sin lógica. |
| **Lombok para entidades** | Evita escribir getters/setters manuales que desordenan el código de la entidad JPA. |
| **application.yml** | Más legible y organizado que `.properties`, permite jerarquía visual clara. |
| **Arquitectura en capas** | `Controller → Service → DAO → Model` separa responsabilidades para un código más mantenible. |
| **H2 en memoria** | No requiere instalación de base de datos externa. Ideal para demostración y desarrollo. |
| **compose.yml sin version** | El campo `version` está deprecado en Docker Compose V2+. |

---

## 📚 Referencias

- [FreeCodeCamp — "What is Idempotence? Explained with Real-World Examples"](https://www.freecodecamp.org/news/idempotence-explained/)
- [Medium — "Idempotency in Software Design"](https://medium.com/@elijahechekwu/idempotency-in-software-design-8b89ef717b23)
- [NetMentor — "La idempotencia en el Desarrollo de Software"](https://www.netmentor.es/entrada/idempotencia-desarrollo-software)

---

## 👨‍🎓 Autor

Proyecto académico para la **Universidad EIA** — Ingeniería de Sistemas y Computación.
