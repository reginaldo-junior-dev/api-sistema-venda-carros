package com.reginaldo.apisistemavendacarros.entity;

import com.reginaldo.apisistemavendacarros.enums.CondicaoCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.enums.TipoCambio;
import com.reginaldo.apisistemavendacarros.enums.TipoCombustivel;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "carro")
public class Carro extends EntidadeBase {

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private BigDecimal preco;

    @Column(nullable = false)
    private String descricao;

    @Column(name = "ano_fabricacao", nullable = false)
    private Integer anoFabricacao;

    @Column(name = "ano_modelo", nullable = false)
    private Integer anoModelo;

    @Column(nullable = false)
    private Integer quilometragem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CondicaoCarro condicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCombustivel combustivel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoCambio cambio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusCarro status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modelo_id", nullable = false)
    private Modelo modelo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cor_id", nullable = false)
    private Cor cor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;
}
