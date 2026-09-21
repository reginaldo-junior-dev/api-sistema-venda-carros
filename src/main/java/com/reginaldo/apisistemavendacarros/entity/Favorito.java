package com.reginaldo.apisistemavendacarros.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "favorito", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cliente_id", "carro_id"})
})
public class Favorito extends EntidadeBase {

    @Column(name = "data_favorito", nullable = false)
    private LocalDateTime dataFavorito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @PrePersist
    public void prePersist() {
        dataFavorito = LocalDateTime.now();
    }
}
