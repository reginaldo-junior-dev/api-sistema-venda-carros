package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Modelo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ModeloRepository extends JpaRepository<Modelo, UUID> {
}
