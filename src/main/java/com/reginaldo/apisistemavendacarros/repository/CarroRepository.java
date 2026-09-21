package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Carro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CarroRepository extends JpaRepository<Carro, UUID> {
}
