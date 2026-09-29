package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Endereco;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EnderecoRepository extends JpaRepository<Endereco, UUID> {

    List<Endereco> findAllByClienteId(UUID clienteId);

    Optional<Endereco> findByIdAndClienteId(UUID id, UUID clienteId);

    Optional<Endereco> findFirstByClienteId(UUID clienteId);

    boolean existsByClienteId(UUID clienteId);

    void deleteByClienteId(UUID clienteId);

    // Executa direto no banco para liberar o índice único uk_endereco_principal antes de marcar outro endereço
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Endereco e SET e.principal = false WHERE e.cliente.id = :clienteId AND e.principal = true")
    void desmarcarPrincipal(@Param("clienteId") UUID clienteId);
}
