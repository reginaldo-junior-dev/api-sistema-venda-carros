package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.enums.StatusCompra;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompraRepository extends JpaRepository<Compra, UUID> {

    boolean existsByCarroIdAndStatusIn(UUID carroId, Collection<StatusCompra> status);

    List<Compra> findByClienteId(UUID clienteId);

    boolean existsByClienteId(UUID clienteId);

    boolean existsByCarroId(UUID carroId);

    // SELECT ... FOR UPDATE: evita aprovar e cancelar a mesma compra ao mesmo tempo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Compra c WHERE c.id = :id")
    Optional<Compra> findByIdComBloqueio(@Param("id") UUID id);
}
