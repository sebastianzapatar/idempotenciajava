package com.eia.idempotencia.dao;

import com.eia.idempotencia.model.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * DAO (Data Access Object) / Repositorio de acceso a datos para la entidad PurchaseOrder.
 *
 * Extiende JpaRepository, lo que proporciona automáticamente métodos CRUD:
 * - save(), findById(), findAll(), deleteById(), etc.
 *
 * Además, definimos un método personalizado para buscar órdenes por su
 * clave de idempotencia, que es el mecanismo central de este ejemplo.
 *
 * Spring Data JPA genera la implementación automáticamente basándose
 * en la convención de nombres del método.
 */
@Repository
public interface PurchaseOrderDao extends JpaRepository<PurchaseOrder, Long> {

    /**
     * Busca una orden de compra por su clave de idempotencia.
     *
     * Este método es la pieza clave de la idempotencia:
     * antes de procesar una nueva solicitud, el servicio usa este método
     * para verificar si ya existe una orden con la misma clave.
     *
     * @param idempotencyKey la clave UUID enviada en el header Idempotency-Key
     * @return Optional con la orden si existe, o vacío si no se ha procesado
     */
    Optional<PurchaseOrder> findByIdempotencyKey(String idempotencyKey);
}
