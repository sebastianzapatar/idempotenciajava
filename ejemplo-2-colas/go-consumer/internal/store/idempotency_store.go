// Package store proporciona un almacen de claves de idempotencia.
//
// En un sistema de produccion, este almacen seria una base de datos
// externa como Redis o PostgreSQL con TTL (tiempo de vida).
// Para este ejemplo educativo, se usa sync.Map que es un mapa
// thread-safe nativo de Go, ideal para acceso concurrente.
package store

import "sync"

// IdempotencyStore es un almacen thread-safe de claves de idempotencia.
// Permite verificar si una transaccion ya fue procesada previamente.
//
// Internamente usa sync.Map, que es mas eficiente que un map + mutex
// cuando las escrituras son poco frecuentes y las lecturas son comunes
// (patron tipico de idempotencia: muchas lecturas, pocas escrituras nuevas).
type IdempotencyStore struct {
	keys sync.Map
}

// New crea una nueva instancia del almacen de idempotencia.
func New() *IdempotencyStore {
	return &IdempotencyStore{}
}

// CheckAndStore verifica si la clave ya fue procesada.
//
// Retorna:
//   - true  si la clave YA existia (duplicado, no procesar de nuevo)
//   - false si la clave es NUEVA (primera vez, procesar normalmente)
//
// Esta operacion es atomica: si dos goroutines llaman simultaneamente
// con la misma clave, solo una recibira false (la primera).
func (s *IdempotencyStore) CheckAndStore(key string) bool {
	_, alreadyExists := s.keys.LoadOrStore(key, true)
	return alreadyExists
}

// Count retorna el numero aproximado de claves almacenadas.
// Util para monitoreo y depuracion.
func (s *IdempotencyStore) Count() int {
	count := 0
	s.keys.Range(func(_, _ interface{}) bool {
		count++
		return true
	})
	return count
}
