package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Marca;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MarcaRepository extends JpaRepository<Marca, UUID> {
}
