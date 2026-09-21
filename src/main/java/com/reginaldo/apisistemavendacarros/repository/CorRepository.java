package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Cor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CorRepository extends JpaRepository<Cor, UUID> {
}
