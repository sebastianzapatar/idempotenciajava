package com.eia.payment.repository;

import com.eia.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Payment.
 *
 * Spring Data JPA genera automaticamente la implementacion de los metodos
 * CRUD basicos (save, findAll, findById, delete, etc.) a partir de la interfaz.
 *
 * Se agrega un metodo personalizado para buscar por paymentId (UUID).
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Busca un pago por su identificador unico (UUID).
     *
     * @param paymentId UUID del pago
     * @return Optional con el pago si existe
     */
    Optional<Payment> findByPaymentId(String paymentId);
}
