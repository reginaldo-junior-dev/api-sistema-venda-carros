package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.reginaldo.apisistemavendacarros.CsrfReal.tokenCsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ParcelaControllerTest {

    private static final LocalDateTime DATA_APROVACAO = LocalDateTime.of(2026, 1, 10, 15, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CompraRepository compraRepository;

    @Autowired
    private PagamentoRepository pagamentoRepository;

    @Autowired
    private ParcelaRepository parcelaRepository;

    @Autowired
    private CarroRepository carroRepository;

    @Autowired
    private MarcaRepository marcaRepository;

    @Autowired
    private ModeloRepository modeloRepository;

    @Autowired
    private CorRepository corRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario;
    private Usuario outroUsuario;
    private Usuario administrador;
    private Cliente cliente;
    private Cliente outroCliente;
    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        usuario = criarUsuario(PerfilUsuario.USUARIO);
        outroUsuario = criarUsuario(PerfilUsuario.USUARIO);
        administrador = criarUsuario(PerfilUsuario.ADMINISTRADOR);
        cliente = criarCliente(usuario, "12345678901");
        outroCliente = criarCliente(outroUsuario, "98765432100");
        pagamento = criarPagamento(cliente, "10000.00", StatusPagamento.APROVADO);
    }

    @Test
    void administradorCriaParcelasConformeExemploDaTarefa() throws Exception {
        criar(administrador, pagamento.getId(), 3)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].numero").value(1))
                .andExpect(jsonPath("$[0].valor").value(3333.33))
                .andExpect(jsonPath("$[0].dataVencimento").value("2026-02-09"))
                .andExpect(jsonPath("$[1].numero").value(2))
                .andExpect(jsonPath("$[1].valor").value(3333.33))
                .andExpect(jsonPath("$[1].dataVencimento").value("2026-03-11"))
                .andExpect(jsonPath("$[2].numero").value(3))
                .andExpect(jsonPath("$[2].valor").value(3333.34))
                .andExpect(jsonPath("$[2].dataVencimento").value("2026-04-10"))
                .andExpect(jsonPath("$[0].pagamentoId").value(pagamento.getId().toString()));
    }

    @Test
    void todasComecamPendentesESemDataDePagamento() throws Exception {
        criar(administrador, pagamento.getId(), 4).andExpect(status().isCreated());

        List<Parcela> parcelas = parcelasDoBanco();
        assertThat(parcelas).hasSize(4);
        assertThat(parcelas).allSatisfy(p -> {
            assertThat(p.getStatus()).isEqualTo(StatusParcela.PENDENTE);
            assertThat(p.getDataPagamento()).isNull();
        });
        assertThat(parcelas).extracting(Parcela::getNumero).containsExactly(1, 2, 3, 4);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 6, 7, 11, 12})
    void somaDasParcelasEhExatamenteOValorDoPagamento(int quantidade) throws Exception {
        Pagamento quebrado = criarPagamento(cliente, "85000.01", StatusPagamento.APROVADO);

        criar(administrador, quebrado.getId(), quantidade).andExpect(status().isCreated());

        recarregarContexto();
        List<Parcela> parcelas = parcelaRepository.findByPagamentoIdOrderByNumeroAsc(quebrado.getId());
        assertThat(parcelas).hasSize(quantidade);
        BigDecimal soma = parcelas.stream().map(Parcela::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma).isEqualByComparingTo("85000.01");

        // Todas iguais, exceto a última, que recebe a sobra de centavos
        BigDecimal base = parcelas.getFirst().getValor();
        assertThat(parcelas.subList(0, quantidade - 1)).allSatisfy(p -> assertThat(p.getValor()).isEqualByComparingTo(base));
        assertThat(parcelas.getLast().getValor()).isGreaterThanOrEqualTo(base);
    }

    @Test
    void quantidadeUmRepresentaPagamentoAVista() throws Exception {
        criar(administrador, pagamento.getId(), 1)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numero").value(1))
                .andExpect(jsonPath("$[0].valor").value(10000.00))
                .andExpect(jsonPath("$[0].dataVencimento").value("2026-02-09"));
    }

    @Test
    void quantidadeDozeFunciona() throws Exception {
        criar(administrador, pagamento.getId(), 12)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(12)))
                .andExpect(jsonPath("$[11].numero").value(12))
                .andExpect(jsonPath("$[11].dataVencimento").value(DATA_APROVACAO.toLocalDate().plusDays(360).toString()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 13})
    void quantidadeInvalidaRetorna400(int quantidade) throws Exception {
        criar(administrador, pagamento.getId(), quantidade)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.quantidade").exists());

        assertThat(parcelaRepository.existsByPagamentoId(pagamento.getId())).isFalse();
    }

    @Test
    void quantidadeAusenteRetorna400() throws Exception {
        criar(administrador, pagamento.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.quantidade").exists());
    }

    @Test
    void pagamentoInexistenteRetorna404() throws Exception {
        criar(administrador, UUID.randomUUID(), 3)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Pagamento não encontrado"));
    }

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = {"PENDENTE", "RECUSADO", "CANCELADO"})
    void pagamentoNaoAprovadoNaoPodeSerParcelado(StatusPagamento statusPagamento) throws Exception {
        Pagamento naoAprovado = criarPagamento(cliente, "10000.00", statusPagamento);

        criar(administrador, naoAprovado.getId(), 3)
                .andExpect(status().isBadRequest());

        assertThat(parcelaRepository.existsByPagamentoId(naoAprovado.getId())).isFalse();
    }

    @Test
    void pagamentoCobradoPelaStripeNaoPodeSerParcelado() throws Exception {
        pagamento.setIdExterno("pi_teste_" + UUID.randomUUID());
        pagamentoRepository.saveAndFlush(pagamento);

        criar(administrador, pagamento.getId(), 3)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Pagamento com cartão já foi cobrado integralmente e não pode ser parcelado"));

        assertThat(parcelaRepository.existsByPagamentoId(pagamento.getId())).isFalse();
    }

    @Test
    void naoPermiteCriarParcelasDuasVezes() throws Exception {
        criar(administrador, pagamento.getId(), 3).andExpect(status().isCreated());

        criar(administrador, pagamento.getId(), 2)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Pagamento já possui parcelas"));

        assertThat(parcelasDoBanco()).hasSize(3);
    }

    @Test
    void camposDeBackendEnviadosNoCorpoSaoIgnorados() throws Exception {
        String body = """
                {"quantidade":2,"pagamentoId":"%s","valor":1.00,"status":"PAGA","numero":9,"dataPagamento":"2000-01-01"}
                """.formatted(UUID.randomUUID());

        criar(administrador, pagamento.getId(), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].pagamentoId").value(pagamento.getId().toString()))
                .andExpect(jsonPath("$[0].numero").value(1))
                .andExpect(jsonPath("$[0].valor").value(5000.00))
                .andExpect(jsonPath("$[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$[0].dataPagamento").isEmpty());
    }

    @Test
    void usuarioComumNaoCriaParcelas() throws Exception {
        criar(usuario, pagamento.getId(), 3)
                .andExpect(status().isForbidden());

        assertThat(parcelaRepository.existsByPagamentoId(pagamento.getId())).isFalse();
    }

    @Test
    void administradorMarcaParcelaComoPaga() throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, StatusParcela.PENDENTE);

        acao(administrador, parcela, "pagar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAGA"))
                .andExpect(jsonPath("$.dataPagamento").value(LocalDate.now().toString()));

        recarregarContexto();
        Parcela paga = parcelaRepository.findById(parcela.getId()).orElseThrow();
        assertThat(paga.getStatus()).isEqualTo(StatusParcela.PAGA);
        assertThat(paga.getDataPagamento()).isEqualTo(LocalDate.now());
    }

    @Test
    void pagarParcelaNaoAlteraStatusDoPagamentoNemDasOutrasParcelas() throws Exception {
        criar(administrador, pagamento.getId(), 3).andExpect(status().isCreated());
        Parcela primeira = parcelasDoBanco().getFirst();

        acao(administrador, primeira, "pagar").andExpect(status().isOk());

        List<Parcela> parcelas = parcelasDoBanco();
        assertThat(parcelas).extracting(Parcela::getStatus)
                .containsExactly(StatusParcela.PAGA, StatusParcela.PENDENTE, StatusParcela.PENDENTE);
        assertThat(pagamentoRepository.findById(pagamento.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusPagamento.APROVADO);
    }

    @ParameterizedTest
    @EnumSource(value = StatusParcela.class, names = {"PAGA", "CANCELADA"})
    void somenteParcelaPendentePodeSerPaga(StatusParcela statusAtual) throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, statusAtual);

        acao(administrador, parcela, "pagar")
                .andExpect(status().isBadRequest());

        recarregarContexto();
        assertThat(parcelaRepository.findById(parcela.getId()).orElseThrow().getStatus()).isEqualTo(statusAtual);
    }

    @Test
    void pagarParcelaInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/parcela/" + UUID.randomUUID() + "/pagar").header("Authorization", bearer(administrador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Parcela não encontrada"));
    }

    @Test
    void administradorCancelaParcelaPendente() throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, StatusParcela.PENDENTE);

        acao(administrador, parcela, "cancelar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.dataPagamento").isEmpty());

        recarregarContexto();
        Parcela cancelada = parcelaRepository.findById(parcela.getId()).orElseThrow();
        assertThat(cancelada.getStatus()).isEqualTo(StatusParcela.CANCELADA);
        assertThat(cancelada.getDataPagamento()).isNull();
    }

    @ParameterizedTest
    @EnumSource(value = StatusParcela.class, names = {"PAGA", "CANCELADA"})
    void somenteParcelaPendentePodeSerCancelada(StatusParcela statusAtual) throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, statusAtual);

        acao(administrador, parcela, "cancelar")
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioComumNaoPagaNemCancelaParcela() throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, StatusParcela.PENDENTE);

        acao(usuario, parcela, "pagar").andExpect(status().isForbidden());
        acao(usuario, parcela, "cancelar").andExpect(status().isForbidden());

        recarregarContexto();
        assertThat(parcelaRepository.findById(parcela.getId()).orElseThrow().getStatus()).isEqualTo(StatusParcela.PENDENTE);
    }

    @Test
    void administradorListaParcelasDeQualquerPagamento() throws Exception {
        criarParcela(pagamento, 2, StatusParcela.PENDENTE);
        criarParcela(pagamento, 1, StatusParcela.PAGA);

        mockMvc.perform(get("/pagamento/" + pagamento.getId() + "/parcelas").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].numero").value(1))
                .andExpect(jsonPath("$[1].numero").value(2));
    }

    @Test
    void administradorConsultaParcela() throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, StatusParcela.PENDENTE);

        mockMvc.perform(get("/parcela/" + parcela.getId()).header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parcela.getId().toString()))
                .andExpect(jsonPath("$.pagamentoId").value(pagamento.getId().toString()));

        mockMvc.perform(get("/parcela/" + UUID.randomUUID()).header("Authorization", bearer(administrador)))
                .andExpect(status().isNotFound());
    }

    @Test
    void clienteConsultaParcelasDoProprioPagamento() throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, StatusParcela.PENDENTE);

        mockMvc.perform(get("/pagamento/" + pagamento.getId() + "/parcelas").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/parcela/" + parcela.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(parcela.getId().toString()));
    }

    @Test
    void clienteNaoConsultaParcelasDeOutroCliente() throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, StatusParcela.PENDENTE);

        mockMvc.perform(get("/pagamento/" + pagamento.getId() + "/parcelas").header("Authorization", bearer(outroUsuario)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/parcela/" + parcela.getId()).header("Authorization", bearer(outroUsuario)))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultasSemAutenticacaoRetornam401() throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, StatusParcela.PENDENTE);

        mockMvc.perform(get("/pagamento/" + pagamento.getId() + "/parcelas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/parcela/" + parcela.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/parcela/" + parcela.getId() + "/pagar").with(tokenCsrf(mockMvc))).andExpect(status().isUnauthorized());
    }

    @Test
    void naoExisteExclusaoDeParcela() throws Exception {
        Parcela parcela = criarParcela(pagamento, 1, StatusParcela.PENDENTE);

        mockMvc.perform(delete("/parcela/" + parcela.getId()).header("Authorization", bearer(administrador)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        assertThat(parcelaRepository.existsById(parcela.getId())).isTrue();
    }

    private ResultActions criar(Usuario autor, UUID pagamentoId, int quantidade) throws Exception {
        return criar(autor, pagamentoId, "{\"quantidade\":%d}".formatted(quantidade));
    }

    private ResultActions criar(Usuario autor, UUID pagamentoId, String body) throws Exception {
        return mockMvc.perform(post("/pagamento/" + pagamentoId + "/parcelas")
                .header("Authorization", bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions acao(Usuario autor, Parcela parcela, String acao) throws Exception {
        return mockMvc.perform(put("/parcela/" + parcela.getId() + "/" + acao)
                .header("Authorization", bearer(autor)));
    }

    private String bearer(Usuario autor) {
        return "Bearer " + jwtService.gerarToken(autor);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private List<Parcela> parcelasDoBanco() {
        recarregarContexto();
        return parcelaRepository.findByPagamentoIdOrderByNumeroAsc(pagamento.getId());
    }

    private Parcela criarParcela(Pagamento alvo, int numero, StatusParcela status) {
        Parcela parcela = new Parcela();
        parcela.setPagamento(alvo);
        parcela.setNumero(numero);
        parcela.setValor(new BigDecimal("1000.00"));
        parcela.setStatus(status);
        parcela.setDataVencimento(LocalDate.of(2026, 2, 9));
        if (status == StatusParcela.PAGA) {
            parcela.setDataPagamento(LocalDate.of(2026, 2, 1));
        }
        return parcelaRepository.saveAndFlush(parcela);
    }

    private Pagamento criarPagamento(Cliente dono, String valor, StatusPagamento status) {
        Carro carro = criarCarro(valor);

        Compra compra = new Compra();
        compra.setCliente(dono);
        compra.setCarro(carro);
        compra.setValorTotal(carro.getPreco());
        compra.setStatus(status == StatusPagamento.APROVADO ? StatusCompra.APROVADA : StatusCompra.PENDENTE);
        compraRepository.saveAndFlush(compra);

        Pagamento novo = new Pagamento();
        novo.setCompra(compra);
        novo.setMetodo(MetodoPagamento.CARTAO_CREDITO);
        novo.setValor(compra.getValorTotal());
        novo.setStatus(status);
        if (status == StatusPagamento.APROVADO) {
            novo.setDataPagamento(DATA_APROVACAO);
        }
        return pagamentoRepository.saveAndFlush(novo);
    }

    private Usuario criarUsuario(PerfilUsuario perfil) {
        Usuario novo = new Usuario();
        novo.setNomeCompleto("Teste " + perfil);
        novo.setEmail(UUID.randomUUID() + "@teste.com");
        novo.setPerfil(perfil);
        novo.setProvedor(ProvedorAutenticacao.LOCAL);
        return usuarioRepository.saveAndFlush(novo);
    }

    private Cliente criarCliente(Usuario dono, String cpf) {
        Cliente novo = new Cliente();
        novo.setUsuario(dono);
        novo.setCpf(cpf);
        novo.setDataNascimento(LocalDate.of(1990, 5, 10));
        novo.setTelefone("11987654321");
        return clienteRepository.saveAndFlush(novo);
    }

    private Carro criarCarro(String preco) {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        Marca marca = new Marca();
        marca.setNome("Marca " + sufixo);
        marcaRepository.save(marca);

        Modelo modelo = new Modelo();
        modelo.setNome("Modelo " + sufixo);
        modelo.setMarca(marca);
        modeloRepository.save(modelo);

        Cor cor = new Cor();
        cor.setNome("Cor " + sufixo);
        corRepository.save(cor);

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + sufixo);
        categoriaRepository.save(categoria);

        Carro novo = new Carro();
        novo.setNome("Carro " + sufixo);
        novo.setPreco(new BigDecimal(preco));
        novo.setDescricao("Carro de teste");
        novo.setAnoFabricacao(2020);
        novo.setAnoModelo(2021);
        novo.setQuilometragem(10000);
        novo.setCondicao(CondicaoCarro.USADO);
        novo.setCombustivel(TipoCombustivel.FLEX);
        novo.setCambio(TipoCambio.MANUAL);
        novo.setStatus(StatusCarro.VENDIDO);
        novo.setModelo(modelo);
        novo.setCor(cor);
        novo.setCategoria(categoria);
        return carroRepository.saveAndFlush(novo);
    }
}
