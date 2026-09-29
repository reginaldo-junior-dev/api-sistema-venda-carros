package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Parcela;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParcelaRepository extends JpaRepository<Parcela, UUID> {

    List<Parcela> findByPagamentoIdOrderByNumeroAsc(UUID pagamentoId);

    boolean existsByPagamentoId(UUID pagamentoId);

    // SELECT ... FOR UPDATE: evita pagar e cancelar a mesma parcela ao mesmo tempo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parcela p WHERE p.id = :id")
    Optional<Parcela> findByIdComBloqueio(@Param("id") UUID id);
}
