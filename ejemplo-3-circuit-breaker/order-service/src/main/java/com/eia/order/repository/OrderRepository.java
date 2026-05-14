package com.eia.order.repository;

import com.eia.order.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad CustomerOrder.
 *
 * Spring Data JPA genera la implementacion automaticamente.
 * Hereda metodos CRUD: save(), findAll(), findById(), delete(), etc.
 */
@Repository
public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {

    /**
     * Busca una orden por su identificador unico (UUID).
     */
    Optional<CustomerOrder> findByOrderId(String orderId);
}
