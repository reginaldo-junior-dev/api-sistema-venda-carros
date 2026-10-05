package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Compra;
import com.reginaldo.apisistemavendacarros.enums.StatusCompra;
import com.reginaldo.apisistemavendacarros.enums.StatusPagamento;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompraRepository extends JpaRepository<Compra, UUID> {

    boolean existsByCarroIdAndStatusIn(UUID carroId, Collection<StatusCompra> status);

    List<Compra> findByClienteId(UUID clienteId);

    Page<Compra> findByClienteId(UUID clienteId, Pageable pageable);

    boolean existsByClienteId(UUID clienteId);

    boolean existsByCarroId(UUID carroId);

    // Compra com pagamento manual pendente não expira: aguarda o administrador
    @Query("""
            SELECT c.id FROM Compra c
            WHERE c.status = :pendente
              AND c.dataCompra < :limite
              AND NOT EXISTS (
                  SELECT p.id FROM Pagamento p
                  WHERE p.compra = c AND p.status = :pagamentoPendente AND p.idExterno IS NULL
              )
            """)
    List<UUID> findIdsPendentesCriadasAntesDe(
            @Param("limite") LocalDateTime limite,
            @Param("pendente") StatusCompra pendente,
            @Param("pagamentoPendente") StatusPagamento pagamentoPendente);

    // SELECT ... FOR UPDATE: evita aprovar e cancelar a mesma compra ao mesmo tempo
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Compra c WHERE c.id = :id")
    Optional<Compra> findByIdComBloqueio(@Param("id") UUID id);
}
