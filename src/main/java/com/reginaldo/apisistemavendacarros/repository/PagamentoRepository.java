package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
}
