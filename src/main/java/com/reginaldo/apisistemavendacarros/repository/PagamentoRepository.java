package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {

    boolean existsByCompraIdAndStatus(UUID compraId, StatusPagamento status);

    List<Pagamento> findByCompraClienteId(UUID clienteId);

    // Com bloqueio: um pagamento recusado/cancelado ao mesmo tempo não é sobrescrito pelo cancelamento da compra
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Pagamento> findByCompraIdAndStatus(UUID compraId, StatusPagamento status);

    @Query("SELECT p.compra.id FROM Pagamento p WHERE p.id = :id")
    Optional<UUID> findCompraIdByPagamentoId(@Param("id") UUID id);

    // SELECT ... FOR UPDATE: evita aprovar, recusar ou cancelar o mesmo pagamento ao mesmo tempo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pagamento p WHERE p.id = :id")
    Optional<Pagamento> findByIdComBloqueio(@Param("id") UUID id);
}
