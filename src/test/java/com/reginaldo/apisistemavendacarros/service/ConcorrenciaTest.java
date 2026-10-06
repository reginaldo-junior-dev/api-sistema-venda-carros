package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

// Requisições realmente simultâneas: os bloqueios e as constraints impedem duplicidade, sem erro 500.
// Sem @Transactional: cada requisição roda na própria transação, como em produção
@SpringBootTest
@AutoConfigureMockMvc
@Import(DadosTeste.class)
class ConcorrenciaTest {

    private static final int THREADS = 5;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private ParcelaRepository parcelaRepository;

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private CarroRepository carroRepository;

    @AfterEach
    void limpar() {
        fabrica.limpar();
    }

    @Test
    void variosClientesComprandoOMesmoCarroGeramUmaUnicaCompra() throws Exception {
        Carro carro = fabrica.carro(StatusCarro.DISPONIVEL);
        List<RequestBuilder> requisicoes = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Usuario comprador = fabrica.usuario(PerfilUsuario.USUARIO);
            fabrica.cliente(comprador);
            requisicoes.add(post("/compra")
                    .header("Authorization", fabrica.bearer(comprador))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"carroId\":\"%s\"}".formatted(carro.getId())));
        }

        List<Integer> status = executarAoMesmoTempo(requisicoes);

        assertThat(status).containsOnlyOnce(201);
        assertThat(status).allMatch(s -> s == 201 || s == 400 || s == 409);
        assertThat(compraRepository.existsByCarroIdAndStatusIn(carro.getId(), List.of(StatusCompra.PENDENTE))).isTrue();
        assertThat(compraRepository.findAll().stream().filter(c -> c.getCarro().getId().equals(carro.getId()))).hasSize(1);
        assertThat(carroRepository.findById(carro.getId()).orElseThrow().getStatus()).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void pagamentosSimultaneosParaMesmaCompraGeramUmUnicoPendente() throws Exception {
        Usuario usuario = fabrica.usuario(PerfilUsuario.USUARIO);
        Cliente cliente = fabrica.cliente(usuario);
        Compra compra = fabrica.compra(cliente, fabrica.carro(StatusCarro.RESERVADO), StatusCompra.PENDENTE);

        List<RequestBuilder> requisicoes = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            requisicoes.add(post("/pagamento")
                    .header("Authorization", fabrica.bearer(usuario))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"compraId\":\"%s\",\"metodo\":\"PIX\"}".formatted(compra.getId())));
        }

        List<Integer> status = executarAoMesmoTempo(requisicoes);

        assertThat(status).containsOnlyOnce(201);
        assertThat(status).filteredOn(s -> s != 201).containsOnly(409);
        assertThat(pagamentoRepository.findByCompraClienteId(cliente.getId())).hasSize(1);
    }

    @Test
    void criacaoSimultaneaDeParcelasGeraUmUnicoParcelamento() throws Exception {
        Usuario administrador = fabrica.usuario(PerfilUsuario.ADMINISTRADOR);
        Cliente cliente = fabrica.cliente(fabrica.usuario(PerfilUsuario.USUARIO));
        Compra compra = fabrica.compra(cliente, fabrica.carro(StatusCarro.VENDIDO), StatusCompra.APROVADA);
        Pagamento pagamento = fabrica.pagamento(compra, StatusPagamento.APROVADO);

        List<RequestBuilder> requisicoes = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            requisicoes.add(post("/pagamento/" + pagamento.getId() + "/parcelas")
                    .header("Authorization", fabrica.bearer(administrador))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantidade\":3}"));
        }

        List<Integer> status = executarAoMesmoTempo(requisicoes);

        assertThat(status).containsOnlyOnce(201);
        assertThat(status).filteredOn(s -> s != 201).containsOnly(409);
        assertThat(parcelaRepository.findByPagamentoIdOrderByNumeroAsc(pagamento.getId())).hasSize(3);
    }

    @Test
    void favoritoSimultaneoSemBloqueioEhBarradoPelaConstraintComo409() throws Exception {
        // Favorito não usa FOR UPDATE: a UNIQUE (cliente_id, carro_id) garante e o TratadorDeExcecoes devolve 409
        Usuario usuario = fabrica.usuario(PerfilUsuario.USUARIO);
        Cliente cliente = fabrica.cliente(usuario);
        Carro carro = fabrica.carro(StatusCarro.DISPONIVEL);

        List<RequestBuilder> requisicoes = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            requisicoes.add(post("/carro/" + carro.getId() + "/favorito").header("Authorization", fabrica.bearer(usuario)));
        }

        List<Integer> status = executarAoMesmoTempo(requisicoes);

        assertThat(status).containsOnlyOnce(201);
        assertThat(status).filteredOn(s -> s != 201).containsOnly(409);
        assertThat(favoritoRepository.findByClienteId(cliente.getId())).hasSize(1);
    }

    @RepeatedTest(5)
    void cancelarCompraEAprovarPagamentoAoMesmoTempoNaoGeraDeadlockNemEstadoMisturado() throws Exception {
        Usuario administrador = fabrica.usuario(PerfilUsuario.ADMINISTRADOR);
        Cliente cliente = fabrica.cliente(fabrica.usuario(PerfilUsuario.USUARIO));
        Carro carro = fabrica.carro(StatusCarro.RESERVADO);
        Compra compra = fabrica.compra(cliente, carro, StatusCompra.PENDENTE);
        Pagamento pagamento = fabrica.pagamento(compra, StatusPagamento.PENDENTE);
        String token = fabrica.bearer(administrador);

        List<Integer> status = executarAoMesmoTempo(List.of(
                put("/compra/" + compra.getId() + "/cancelar").header("Authorization", token),
                put("/pagamento/" + pagamento.getId() + "/aprovar").header("Authorization", token)
        ));

        // Uma operação vence e a outra recebe 400. Nunca 500 (deadlock) nem as duas com sucesso
        assertThat(status).containsExactlyInAnyOrder(200, 400);

        StatusCompra statusCompra = compraRepository.findById(compra.getId()).orElseThrow().getStatus();
        StatusPagamento statusPagamento = pagamentoRepository.findById(pagamento.getId()).orElseThrow().getStatus();
        StatusCarro statusCarro = carroRepository.findById(carro.getId()).orElseThrow().getStatus();

        if (statusCompra == StatusCompra.APROVADA) {
            assertThat(statusPagamento).isEqualTo(StatusPagamento.APROVADO);
            assertThat(statusCarro).isEqualTo(StatusCarro.VENDIDO);
        } else {
            assertThat(statusCompra).isEqualTo(StatusCompra.CANCELADA);
            assertThat(statusPagamento).isEqualTo(StatusPagamento.CANCELADO);
            assertThat(statusCarro).isEqualTo(StatusCarro.DISPONIVEL);
        }
    }

    // As threads são liberadas juntas para maximizar a disputa
    private List<Integer> executarAoMesmoTempo(List<RequestBuilder> requisicoes) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(requisicoes.size());
        CountDownLatch largada = new CountDownLatch(1);
        try {
            List<Future<Integer>> futuros = new ArrayList<>();
            for (RequestBuilder requisicao : requisicoes) {
                futuros.add(executor.submit(() -> {
                    largada.await();
                    return mockMvc.perform(requisicao).andReturn().getResponse().getStatus();
                }));
            }
            largada.countDown();

            List<Integer> status = new ArrayList<>();
            for (Future<Integer> futuro : futuros) {
                status.add(futuro.get(30, TimeUnit.SECONDS));
            }
            return status;
        } finally {
            executor.shutdownNow();
        }
    }
}
