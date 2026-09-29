package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.ImagemCarro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImagemCarroRepository extends JpaRepository<ImagemCarro, UUID> {

    boolean existsByCarroId(UUID carroId);

    int countByCarroId(UUID carroId);

    Optional<ImagemCarro> findFirstByCarroIdOrderByOrdemAsc(UUID carroId);
}
