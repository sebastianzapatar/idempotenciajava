# ============================================================
# Dockerfile para el servicio Go Consumidor
# ============================================================
# Multi-stage build:
# 1. Compilar el binario Go desde cmd/consumer/main.go
# 2. Copiar solo el binario a una imagen minima (alpine)
# ============================================================

FROM golang:1.22-alpine AS build
WORKDIR /app

# Copiar archivos de dependencias primero (para cache de Docker)
COPY go.mod go.sum ./
RUN go mod download

# Copiar el codigo fuente completo
COPY . .

# Compilar el binario desde el paquete cmd/consumer
RUN CGO_ENABLED=0 GOOS=linux go build -o /consumer ./cmd/consumer

# ---- Imagen final (liviana) ----
FROM alpine:3.19
WORKDIR /app
COPY --from=build /consumer .
ENTRYPOINT ["./consumer"]
