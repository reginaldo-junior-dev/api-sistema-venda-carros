package com.reginaldo.apisistemavendacarros.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "imagem_carro")
public class ImagemCarro extends EntidadeBase {

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private Integer ordem;

    @Column(nullable = false)
    private Boolean principal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carro_id", nullable = false)
    private Carro carro;
}
