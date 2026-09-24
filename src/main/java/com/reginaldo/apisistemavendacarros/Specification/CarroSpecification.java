package com.reginaldo.apisistemavendacarros.Specification;

import com.reginaldo.apisistemavendacarros.dto.CarroFiltro;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.enums.CondicaoCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.enums.TipoCambio;
import com.reginaldo.apisistemavendacarros.enums.TipoCombustivel;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.UUID;

public class CarroSpecification {
    public static Specification<Carro> comNome (String nome) {
        return (root, query, criteriaBuilder) -> {
            if (nome == null || nome.isBlank()) {
                return null;
            }
           return criteriaBuilder.like(
                    criteriaBuilder.upper(root.get("nome")),
                    "%" + nome.toUpperCase() + "%"
            );
        };
    }

    public static Specification<Carro> comCategoria (UUID categoriaId) {
        return (root, query, criteriaBuilder) -> {
            if (categoriaId == null) {
                return null;
            }
           return criteriaBuilder.equal(
                    root.get("categoria").get("id"),
                    categoriaId
                    );
        };
    }

    public static Specification<Carro> comMarca (UUID marcaId) {
        return (root, query, criteriaBuilder) -> {
          if (marcaId == null) {
              return null;
          }
          return criteriaBuilder.equal(
                  root.get("modelo").get("marca").get("id"),
                  marcaId
          );
        };
    }

    public static Specification<Carro> comModelo (UUID modeloId) {
        return (root, query, criteriaBuilder) -> {
          if (modeloId == null) {
              return null;
          }
          return criteriaBuilder.equal(
            root.get("modelo").get("id"),
            modeloId
          );
        };
    }

    public static Specification<Carro> comCor (UUID corId) {
        return (root, query, criteriaBuilder) -> {
          if (corId == null) {
              return null;
          }
          return criteriaBuilder.equal(
                  root.get("cor").get("id"),
                  corId
          );
        };
    }

    public static Specification<Carro> comCondicao (String condicao) {
        return (root, query, criteriaBuilder) -> {
          if (condicao == null || condicao.isBlank()) {
              return null;
          }
          try {
              return criteriaBuilder.equal(
                root.get("condicao"),
                      CondicaoCarro.valueOf(condicao.toUpperCase())
              );
          } catch (IllegalArgumentException e) {
              throw new ValorInvalidoException("Condição inválida. Use NOVO ou USADO: " + condicao);
          }
        };
    }

    public static Specification<Carro> comCombustivel (String combustivel) {
        return (root, query, criteriaBuilder) -> {
          if (combustivel == null || combustivel.isBlank()) {
              return null;
          }
          try {
              return criteriaBuilder.equal(
                root.get("combustivel"),
                      TipoCombustivel.valueOf(combustivel.toUpperCase())
              );
          } catch (IllegalArgumentException e) {
              throw new ValorInvalidoException("Combustível inválido. Use GASOLINA, ETANOL, DIESEL, GNV, HIBRIDO ou FLEX: " + combustivel);
          }
        };
    }

    public static Specification<Carro> comCambio (String cambio) {
        return (root, query, criteriaBuilder) -> {
          if (cambio == null || cambio.isBlank()) {
              return null;
          }
          try {
              return criteriaBuilder.equal(
                root.get("cambio"),
                      TipoCambio.valueOf(cambio.toUpperCase())
              );
          } catch (IllegalArgumentException e) {
              throw new ValorInvalidoException("Câmbio inválido. Use MANUAL ou AUTOMATICO: " + cambio);
          }
        };
    }

    public static Specification<Carro> comStatus (String status) {
        return (root, query, criteriaBuilder) -> {
          if (status == null || status.isBlank()) {
              return null;
          }
          try {
              return criteriaBuilder.equal(
                root.get("status"),
                      StatusCarro.valueOf(status.toUpperCase())
              );
          } catch (IllegalArgumentException e) {
              throw new ValorInvalidoException("Status inválido. Use DISPONIVEL, RESERVADO ou VENDIDO: " + status);
          }
        };
    }

    public static Specification<Carro> comPrecoMin (BigDecimal preco) {
        return (root, query, criteriaBuilder) -> {
            if (preco == null) {
                return null;
            }
           return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("preco"),
                    preco
            );
        };
    }

    public static Specification<Carro> comPrecoMax (BigDecimal preco) {
        return (root, query, criteriaBuilder) -> {
            if (preco == null) {
                return null;
            }
           return criteriaBuilder.lessThanOrEqualTo(
                    root.get("preco"),
                    preco
            );
        };
    }

    public static Specification<Carro> comAnoMin (Integer ano) {
        return (root, query, criteriaBuilder) -> {
            if (ano == null) {
                return null;
            }
           return criteriaBuilder.greaterThanOrEqualTo(
                    root.get("anoModelo"),
                    ano
            );
        };
    }

    public static Specification<Carro> comAnoMax (Integer ano) {
        return (root, query, criteriaBuilder) -> {
            if (ano == null) {
                return null;
            }
           return criteriaBuilder.lessThanOrEqualTo(
                    root.get("anoModelo"),
                    ano
            );
        };
    }

    public static Specification<Carro> comQuilometragemMax (Integer quilometragem) {
        return (root, query, criteriaBuilder) -> {
            if (quilometragem == null) {
                return null;
            }
           return criteriaBuilder.lessThanOrEqualTo(
                    root.get("quilometragem"),
                    quilometragem
            );
        };
    }

    public static Specification<Carro> filtrar (CarroFiltro filtro) {
        return Specification.<Carro>unrestricted()
                .and(comNome(filtro.nome()))
                .and(comCategoria(filtro.categoriaId()))
                .and(comMarca(filtro.marcaId()))
                .and(comModelo(filtro.modeloId()))
                .and(comCor(filtro.corId()))
                .and(comCondicao(filtro.condicao()))
                .and(comCombustivel(filtro.combustivel()))
                .and(comCambio(filtro.cambio()))
                .and(comStatus(filtro.status()))
                .and(comPrecoMin(filtro.precoMin()))
                .and(comPrecoMax(filtro.precoMax()))
                .and(comAnoMin(filtro.anoMin()))
                .and(comAnoMax(filtro.anoMax()))
                .and(comQuilometragemMax(filtro.quilometragemMax()));
    }
}
