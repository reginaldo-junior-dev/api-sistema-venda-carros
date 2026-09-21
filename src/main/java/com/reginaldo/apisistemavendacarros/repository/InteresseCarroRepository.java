package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.InteresseCarro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InteresseCarroRepository extends JpaRepository<InteresseCarro, UUID> {
}
