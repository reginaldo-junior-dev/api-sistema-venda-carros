package com.reginaldo.apisistemavendacarros.entity;

import com.reginaldo.apisistemavendacarros.enums.StatusInteresse;
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
@Table(name = "interesse_carro")
public class InteresseCarro extends EntidadeBase {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String telefone;

    @Column(nullable = false)
    private String mensagem;

    @Column(name = "data_interesse", nullable = false)
    private LocalDateTime dataInteresse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusInteresse status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @PrePersist
    public void prePersist() {
        dataInteresse = LocalDateTime.now();
    }
}
