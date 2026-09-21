package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Compra;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CompraRepository extends JpaRepository<Compra, UUID> {
}
