package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Favorito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FavoritoRepository extends JpaRepository<Favorito, UUID> {

    Optional<Favorito> findByClienteIdAndCarroId(UUID clienteId, UUID carroId);

    boolean existsByClienteIdAndCarroId(UUID clienteId, UUID carroId);

    List<Favorito> findByClienteId(UUID clienteId);

    Page<Favorito> findByClienteId(UUID clienteId, Pageable pageable);

    void deleteByClienteId(UUID clienteId);

    void deleteByCarroId(UUID carroId);
}
