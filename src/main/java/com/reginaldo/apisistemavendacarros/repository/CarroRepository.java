package com.reginaldo.apisistemavendacarros.repository;

import com.reginaldo.apisistemavendacarros.entity.Carro;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CarroRepository extends JpaRepository<Carro, UUID>,
        JpaSpecificationExecutor<Carro> {

    // SELECT ... FOR UPDATE: requisições simultâneas para o mesmo carro esperam a transação atual
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Carro c WHERE c.id = :id")
    Optional<Carro> findByIdComBloqueio(@Param("id") UUID id);
}
