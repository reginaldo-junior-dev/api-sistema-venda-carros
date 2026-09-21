package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Favorito;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FavoritoRepository extends JpaRepository<Favorito, UUID> {
}
