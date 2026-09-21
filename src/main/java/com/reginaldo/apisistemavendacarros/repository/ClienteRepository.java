package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {
}
