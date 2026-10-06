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
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PagamentoControllerTest {

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
    private Carro carro;
    private Compra compra;

    @BeforeEach
    void setUp() {
        usuario = criarUsuario(PerfilUsuario.USUARIO);
        outroUsuario = criarUsuario(PerfilUsuario.USUARIO);
        administrador = criarUsuario(PerfilUsuario.ADMINISTRADOR);
        cliente = criarCliente(usuario, "12345678901");
        outroCliente = criarCliente(outroUsuario, "98765432100");
        carro = criarCarro(StatusCarro.RESERVADO, "85000.00");
        compra = criarCompra(cliente, carro, StatusCompra.PENDENTE);
    }

    @Test
    void usuarioAutenticadoCriaPagamento() throws Exception {
        pagar(usuario, compra.getId(), "PIX")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.compraId").value(compra.getId().toString()))
                .andExpect(jsonPath("$.valor").value(85000.00))
                .andExpect(jsonPath("$.metodo").value("PIX"))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.idExterno").isEmpty())
                .andExpect(jsonPath("$.dataPagamento").isEmpty());

        recarregarContexto();
        Pagamento salvo = pagamentoRepository.findByCompraClienteId(cliente.getId()).getFirst();
        assertThat(salvo.getValor()).isEqualByComparingTo("85000.00");
        assertThat(salvo.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(salvo.getIdExterno()).isNull();
        assertThat(salvo.getDataPagamento()).isNull();
    }

    @ParameterizedTest
    @EnumSource(MetodoPagamento.class)
    void aceitaTodosOsMetodos(MetodoPagamento metodo) throws Exception {
        pagar(usuario, compra.getId(), metodo.name())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metodo").value(metodo.name()));
    }

    @Test
    void camposDeBackendEnviadosNoCorpoSaoIgnorados() throws Exception {
        String body = """
                {"compraId":"%s","metodo":"PIX","valor":1.00,"status":"APROVADO","idExterno":"mp-123",
                 "dataPagamento":"2000-01-01T00:00:00","clienteId":"%s"}
                """.formatted(compra.getId(), outroCliente.getId());

        pagar(usuario, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value(85000.00))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.idExterno").isEmpty())
                .andExpect(jsonPath("$.dataPagamento").isEmpty());
    }

    @Test
    void compraInexistenteRetorna404() throws Exception {
        pagar(usuario, UUID.randomUUID(), "PIX")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Compra não encontrada"));
    }

    @Test
    void usuarioNaoDonoDaCompraRetorna403() throws Exception {
        pagar(outroUsuario, compra.getId(), "PIX")
                .andExpect(status().isForbidden());

        assertThat(pagamentoRepository.existsByCompraIdAndStatus(compra.getId(), StatusPagamento.PENDENTE)).isFalse();
    }

    @Test
    void compraAprovadaNaoPermitePagamento() throws Exception {
        Compra aprovada = criarCompra(cliente, criarCarro(StatusCarro.VENDIDO, "50000.00"), StatusCompra.APROVADA);

        pagar(usuario, aprovada.getId(), "PIX")
                .andExpect(status().isBadRequest());
    }

    @Test
    void compraCanceladaNaoPermitePagamento() throws Exception {
        Compra cancelada = criarCompra(cliente, criarCarro(StatusCarro.DISPONIVEL, "50000.00"), StatusCompra.CANCELADA);

        pagar(usuario, cancelada.getId(), "PIX")
                .andExpect(status().isBadRequest());
    }

    @Test
    void naoPermiteDoisPagamentosPendentesParaMesmaCompra() throws Exception {
        pagar(usuario, compra.getId(), "PIX").andExpect(status().isCreated());

        pagar(usuario, compra.getId(), "BOLETO")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Compra já possui um pagamento pendente"));

        assertThat(pagamentoRepository.findByCompraClienteId(cliente.getId())).hasSize(1);
    }

    @Test
    void pagamentoRecusadoOuCanceladoNaoImpedeNovaTentativa() throws Exception {
        criarPagamento(compra, StatusPagamento.RECUSADO);
        criarPagamento(compra, StatusPagamento.CANCELADO);

        pagar(usuario, compra.getId(), "CARTAO_CREDITO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void metodoOuCompraAusentesRetornam400() throws Exception {
        pagar(usuario, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.compraId").exists())
                .andExpect(jsonPath("$.mensagens.metodo").exists());
    }

    @Test
    void criarSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(post("/pagamento").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"compraId\":\"%s\",\"metodo\":\"PIX\"}".formatted(compra.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorAprovaPagamentoPendente() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        acao(administrador, pagamento, "aprovar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.dataPagamento").exists())
                .andExpect(jsonPath("$.idExterno").isEmpty());

        recarregarContexto();
        Pagamento aprovado = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
        assertThat(aprovado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(aprovado.getDataPagamento()).isAfter(antes);
        assertThat(aprovado.getIdExterno()).isNull();
        assertThat(statusDaCompra()).isEqualTo(StatusCompra.APROVADA);
        assertThat(statusDoCarro()).isEqualTo(StatusCarro.VENDIDO);
    }

    @Test
    void pagamentoAprovadoEncerraNovosPagamentos() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);
        acao(administrador, pagamento, "aprovar").andExpect(status().isOk());

        pagar(usuario, compra.getId(), "PIX")
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @EnumSource(value = StatusPagamento.class, names = {"APROVADO", "RECUSADO", "CANCELADO"})
    void somentePagamentoPendentePodeSerAprovado(StatusPagamento statusAtual) throws Exception {
        Pagamento pagamento = criarPagamento(compra, statusAtual);

        acao(administrador, pagamento, "aprovar")
                .andExpect(status().isBadRequest());

        assertThat(statusDaCompra()).isEqualTo(StatusCompra.PENDENTE);
    }

    @Test
    void aprovarPagamentoInexistenteRetorna404() throws Exception {
        mockMvc.perform(put("/pagamento/" + UUID.randomUUID() + "/aprovar").header("Authorization", bearer(administrador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Pagamento não encontrado"));
    }

    @Test
    void administradorRecusaPagamentoPendente() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        acao(administrador, pagamento, "recusar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.dataPagamento").isEmpty());

        recarregarContexto();
        assertThat(pagamentoRepository.findById(pagamento.getId()).orElseThrow().getDataPagamento()).isNull();
        assertThat(statusDaCompra()).isEqualTo(StatusCompra.PENDENTE);
        assertThat(statusDoCarro()).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void recusaPermiteNovoPagamentoComOutroMetodo() throws Exception {
        Pagamento pix = criarPagamento(compra, StatusPagamento.PENDENTE);
        acao(administrador, pix, "recusar").andExpect(status().isOk());

        pagar(usuario, compra.getId(), "CARTAO_CREDITO")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.metodo").value("CARTAO_CREDITO"))
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void pagamentoAprovadoNaoPodeSerRecusado() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.APROVADO);

        acao(administrador, pagamento, "recusar")
                .andExpect(status().isBadRequest());
    }

    @Test
    void administradorCancelaPagamentoPendente() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        acao(administrador, pagamento, "cancelar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"))
                .andExpect(jsonPath("$.dataPagamento").isEmpty());

        recarregarContexto();
        assertThat(pagamentoRepository.findById(pagamento.getId()).orElseThrow().getDataPagamento()).isNull();
        assertThat(statusDaCompra()).isEqualTo(StatusCompra.PENDENTE);
        assertThat(statusDoCarro()).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void pagamentoAprovadoNaoPodeSerCancelado() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.APROVADO);

        acao(administrador, pagamento, "cancelar")
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioComumNaoAprovaRecusaNemCancela() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        acao(usuario, pagamento, "aprovar").andExpect(status().isForbidden());
        acao(usuario, pagamento, "recusar").andExpect(status().isForbidden());
        acao(usuario, pagamento, "cancelar").andExpect(status().isForbidden());

        recarregarContexto();
        assertThat(pagamentoRepository.findById(pagamento.getId()).orElseThrow().getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(statusDaCompra()).isEqualTo(StatusCompra.PENDENTE);
    }

    @Test
    void clienteListaSomenteSeusPagamentos() throws Exception {
        criarPagamento(compra, StatusPagamento.RECUSADO);
        criarPagamento(compra, StatusPagamento.PENDENTE);
        Compra compraDeOutro = criarCompra(outroCliente, criarCarro(StatusCarro.RESERVADO, "30000.00"), StatusCompra.PENDENTE);
        criarPagamento(compraDeOutro, StatusPagamento.PENDENTE);

        mockMvc.perform(get("/cliente/me/pagamentos").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].compraId").value(compra.getId().toString()))
                .andExpect(jsonPath("$.content[1].compraId").value(compra.getId().toString()));
    }

    @Test
    void listaVaziaRetornaNormalmente() throws Exception {
        mockMvc.perform(get("/cliente/me/pagamentos").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void clienteNaoConsultaPagamentosPelasRotasAdministrativas() throws Exception {
        Compra compraDeOutro = criarCompra(outroCliente, criarCarro(StatusCarro.RESERVADO, "30000.00"), StatusCompra.PENDENTE);
        Pagamento deOutro = criarPagamento(compraDeOutro, StatusPagamento.PENDENTE);

        mockMvc.perform(get("/pagamento/" + deOutro.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/pagamento").header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorListaPagamentos() throws Exception {
        Pagamento meu = criarPagamento(compra, StatusPagamento.PENDENTE);
        Compra compraDeOutro = criarCompra(outroCliente, criarCarro(StatusCarro.RESERVADO, "30000.00"), StatusCompra.PENDENTE);
        Pagamento deOutro = criarPagamento(compraDeOutro, StatusPagamento.PENDENTE);

        mockMvc.perform(get("/pagamento").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(meu.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(deOutro.getId())).exists());
    }

    @Test
    void administradorBuscaPagamentoPorId() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        mockMvc.perform(get("/pagamento/" + pagamento.getId()).header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(pagamento.getId().toString()))
                .andExpect(jsonPath("$.compraId").value(compra.getId().toString()));

        mockMvc.perform(get("/pagamento/" + UUID.randomUUID()).header("Authorization", bearer(administrador)))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultasSemAutenticacaoRetornam401() throws Exception {
        mockMvc.perform(get("/cliente/me/pagamentos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pagamento")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pagamento/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void naoExisteExclusaoDePagamento() throws Exception {
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        mockMvc.perform(delete("/pagamento/" + pagamento.getId()).header("Authorization", bearer(administrador)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        assertThat(pagamentoRepository.existsById(pagamento.getId())).isTrue();
    }

    private ResultActions pagar(Usuario autor, UUID compraId, String metodo) throws Exception {
        return pagar(autor, "{\"compraId\":\"%s\",\"metodo\":\"%s\"}".formatted(compraId, metodo));
    }

    private ResultActions pagar(Usuario autor, String body) throws Exception {
        return mockMvc.perform(post("/pagamento")
                .header("Authorization", bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions acao(Usuario autor, Pagamento pagamento, String acao) throws Exception {
        return mockMvc.perform(put("/pagamento/" + pagamento.getId() + "/" + acao)
                .header("Authorization", bearer(autor)));
    }

    private String bearer(Usuario autor) {
        return "Bearer " + jwtService.gerarToken(autor);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private StatusCompra statusDaCompra() {
        recarregarContexto();
        return compraRepository.findById(compra.getId()).orElseThrow().getStatus();
    }

    private StatusCarro statusDoCarro() {
        recarregarContexto();
        return carroRepository.findById(carro.getId()).orElseThrow().getStatus();
    }

    private Pagamento criarPagamento(Compra alvo, StatusPagamento status) {
        Pagamento pagamento = new Pagamento();
        pagamento.setCompra(alvo);
        pagamento.setMetodo(MetodoPagamento.PIX);
        pagamento.setValor(alvo.getValorTotal());
        pagamento.setStatus(status);
        if (status == StatusPagamento.APROVADO) {
            pagamento.setDataPagamento(LocalDateTime.now());
        }
        return pagamentoRepository.saveAndFlush(pagamento);
    }

    private Compra criarCompra(Cliente dono, Carro alvo, StatusCompra status) {
        Compra nova = new Compra();
        nova.setCliente(dono);
        nova.setCarro(alvo);
        nova.setValorTotal(alvo.getPreco());
        nova.setStatus(status);
        return compraRepository.saveAndFlush(nova);
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

    private Carro criarCarro(StatusCarro status, String preco) {
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
        novo.setStatus(status);
        novo.setModelo(modelo);
        novo.setCor(cor);
        novo.setCategoria(categoria);
        return carroRepository.saveAndFlush(novo);
    }
}
