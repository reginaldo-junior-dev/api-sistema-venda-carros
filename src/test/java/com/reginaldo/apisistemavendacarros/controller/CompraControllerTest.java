package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class CompraControllerTest {

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

    @BeforeEach
    void setUp() {
        usuario = criarUsuario(PerfilUsuario.USUARIO);
        outroUsuario = criarUsuario(PerfilUsuario.USUARIO);
        administrador = criarUsuario(PerfilUsuario.ADMINISTRADOR);
        cliente = criarCliente(usuario, "12345678901");
        outroCliente = criarCliente(outroUsuario, "98765432100");
        carro = criarCarro(StatusCarro.DISPONIVEL, "85000.00");
    }

    @Test
    void usuarioAutenticadoCriaCompra() throws Exception {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        comprar(usuario, carro.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.carroId").value(carro.getId().toString()))
                .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.valorTotal").value(85000.00))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dataCompra").exists());

        recarregarContexto();
        Compra salva = compraRepository.findByClienteId(cliente.getId()).getFirst();
        assertThat(salva.getStatus()).isEqualTo(StatusCompra.PENDENTE);
        assertThat(salva.getValorTotal()).isEqualByComparingTo("85000.00");
        assertThat(salva.getDataCompra()).isAfter(antes).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(statusDoCarro(carro)).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void camposDeBackendEnviadosNoCorpoSaoIgnorados() throws Exception {
        String body = """
                {"carroId":"%s","clienteId":"%s","valorTotal":1.00,"status":"APROVADA","dataCompra":"2000-01-01T00:00:00"}
                """.formatted(carro.getId(), outroCliente.getId());

        comprar(usuario, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.valorTotal").value(85000.00))
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        assertThat(compraRepository.findByClienteId(outroCliente.getId())).isEmpty();
        assertThat(compraRepository.findByClienteId(cliente.getId()).getFirst().getDataCompra().getYear())
                .isNotEqualTo(2000);
    }

    @Test
    void valorDaCompraNaoMudaQuandoPrecoDoCarroMuda() throws Exception {
        comprar(usuario, carro.getId()).andExpect(status().isCreated());

        recarregarContexto();
        Carro atualizado = carroRepository.findById(carro.getId()).orElseThrow();
        atualizado.setPreco(new BigDecimal("99000.00"));
        recarregarContexto();

        assertThat(compraRepository.findByClienteId(cliente.getId()).getFirst().getValorTotal())
                .isEqualByComparingTo("85000.00");
    }

    @Test
    void carroInexistenteRetorna404() throws Exception {
        comprar(usuario, UUID.randomUUID())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Carro não encontrado"));
    }

    @Test
    void carroReservadoRetorna400() throws Exception {
        Carro reservado = criarCarro(StatusCarro.RESERVADO, "50000.00");

        comprar(usuario, reservado.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Carro não está disponível para compra"));
    }

    @Test
    void carroVendidoRetorna400() throws Exception {
        Carro vendido = criarCarro(StatusCarro.VENDIDO, "50000.00");

        comprar(usuario, vendido.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Carro não está disponível para compra"));
    }

    @Test
    void segundaCompraParaMesmoCarroRetorna400() throws Exception {
        comprar(usuario, carro.getId()).andExpect(status().isCreated());

        comprar(outroUsuario, carro.getId())
                .andExpect(status().isBadRequest());

        assertThat(compraRepository.findByClienteId(outroCliente.getId())).isEmpty();
    }

    @Test
    void compraAtivaImpedeNovaCompraMesmoComCarroDisponivel() throws Exception {
        // Estado inconsistente (ex.: carro alterado manualmente): a regra da compra ativa ainda bloqueia
        criarCompra(outroCliente, carro, StatusCompra.PENDENTE);

        comprar(usuario, carro.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Carro já possui uma compra em andamento"));
    }

    @Test
    void compraAprovadaImpedeNovaCompra() throws Exception {
        // Carro DISPONIVEL de propósito: o bloqueio tem que vir da regra de compra ativa
        criarCompra(outroCliente, carro, StatusCompra.APROVADA);

        comprar(usuario, carro.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Carro já possui uma compra em andamento"));
    }

    @Test
    void compraCanceladaNaoImpedeNovaCompra() throws Exception {
        criarCompra(outroCliente, carro, StatusCompra.CANCELADA);

        comprar(usuario, carro.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"));
    }

    @Test
    void usuarioSemClienteRetorna404() throws Exception {
        Usuario semCliente = criarUsuario(PerfilUsuario.USUARIO);

        comprar(semCliente, carro.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado"));

        assertThat(statusDoCarro(carro)).isEqualTo(StatusCarro.DISPONIVEL);
    }

    @Test
    void carroIdAusenteRetorna400() throws Exception {
        comprar(usuario, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.carroId").exists());
    }

    @Test
    void criarSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(post("/compra").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carroId\":\"%s\"}".formatted(carro.getId())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void naoExisteEndpointDeAprovacaoDiretaDaCompra() throws Exception {
        Compra compra = criarCompraPendenteComCarroReservado();

        mockMvc.perform(put("/compra/" + compra.getId() + "/aprovar").header("Authorization", bearer(administrador)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        recarregarContexto();
        assertThat(compraRepository.findById(compra.getId()).orElseThrow().getStatus()).isEqualTo(StatusCompra.PENDENTE);
        assertThat(statusDoCarro(carro)).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void aprovacaoDoPagamentoAprovaCompraEVendeCarro() throws Exception {
        Compra compra = criarCompraPendenteComCarroReservado();
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        aprovarPagamento(pagamento).andExpect(status().isOk());

        recarregarContexto();
        assertThat(compraRepository.findById(compra.getId()).orElseThrow().getStatus()).isEqualTo(StatusCompra.APROVADA);
        assertThat(statusDoCarro(carro)).isEqualTo(StatusCarro.VENDIDO);
    }

    @Test
    void compraAprovadaNaoPodeSerAprovadaNovamente() throws Exception {
        Compra compra = criarCompra(cliente, carro, StatusCompra.APROVADA);
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        aprovarPagamento(pagamento).andExpect(status().isBadRequest());
    }

    @Test
    void compraCanceladaNaoPodeSerAprovada() throws Exception {
        Compra compra = criarCompra(cliente, carro, StatusCompra.CANCELADA);
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        aprovarPagamento(pagamento).andExpect(status().isBadRequest());
    }

    @Test
    void aprovarCompraComCarroNaoReservadoRetorna400() throws Exception {
        Compra compra = criarCompra(cliente, carro, StatusCompra.PENDENTE);
        Pagamento pagamento = criarPagamento(compra, StatusPagamento.PENDENTE);

        aprovarPagamento(pagamento)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Carro da compra não está reservado"));
    }

    @Test
    void administradorCancelaCompraPendente() throws Exception {
        Compra compra = criarCompraPendenteComCarroReservado();

        mockMvc.perform(put("/compra/" + compra.getId() + "/cancelar").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"));

        recarregarContexto();
        assertThat(compraRepository.findById(compra.getId()).orElseThrow().getStatus()).isEqualTo(StatusCompra.CANCELADA);
        assertThat(statusDoCarro(carro)).isEqualTo(StatusCarro.DISPONIVEL);
    }

    @Test
    void cancelarCompraCancelaPagamentosPendentes() throws Exception {
        Compra compra = criarCompraPendenteComCarroReservado();
        Pagamento recusado = criarPagamento(compra, StatusPagamento.RECUSADO);
        Pagamento pendente = criarPagamento(compra, StatusPagamento.PENDENTE);

        mockMvc.perform(put("/compra/" + compra.getId() + "/cancelar").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk());

        recarregarContexto();
        assertThat(pagamentoRepository.findById(pendente.getId()).orElseThrow().getStatus()).isEqualTo(StatusPagamento.CANCELADO);
        assertThat(pagamentoRepository.findById(recusado.getId()).orElseThrow().getStatus()).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(statusDoCarro(carro)).isEqualTo(StatusCarro.DISPONIVEL);

        aprovarPagamento(pendente).andExpect(status().isBadRequest());
    }

    @Test
    void compraAprovadaNaoPodeSerCancelada() throws Exception {
        Carro vendido = criarCarroComCompraAprovada();
        Compra compra = compraRepository.findAll().stream()
                .filter(c -> c.getCarro().getId().equals(vendido.getId()))
                .findFirst().orElseThrow();

        mockMvc.perform(put("/compra/" + compra.getId() + "/cancelar").header("Authorization", bearer(administrador)))
                .andExpect(status().isBadRequest());

        assertThat(statusDoCarro(vendido)).isEqualTo(StatusCarro.VENDIDO);
    }

    @Test
    void compraCanceladaNaoPodeSerCanceladaNovamente() throws Exception {
        Compra compra = criarCompra(cliente, carro, StatusCompra.CANCELADA);

        mockMvc.perform(put("/compra/" + compra.getId() + "/cancelar").header("Authorization", bearer(administrador)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioComumNaoCancelaCompra() throws Exception {
        Compra compra = criarCompraPendenteComCarroReservado();

        mockMvc.perform(put("/compra/" + compra.getId() + "/cancelar").header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());

        assertThat(statusDoCarro(carro)).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void clienteListaSomenteSuasCompras() throws Exception {
        criarCompra(cliente, carro, StatusCompra.CANCELADA);
        criarCompra(cliente, criarCarro(StatusCarro.DISPONIVEL, "40000.00"), StatusCompra.CANCELADA);
        criarCompra(outroCliente, criarCarro(StatusCarro.DISPONIVEL, "30000.00"), StatusCompra.CANCELADA);

        mockMvc.perform(get("/cliente/me/compras").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].clienteId").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.content[1].clienteId").value(cliente.getId().toString()));
    }

    @Test
    void listaDeComprasEPaginadaComMetadados() throws Exception {
        criarCompra(cliente, carro, StatusCompra.CANCELADA);
        criarCompra(cliente, criarCarro(StatusCarro.DISPONIVEL, "40000.00"), StatusCompra.CANCELADA);
        criarCompra(cliente, criarCarro(StatusCarro.DISPONIVEL, "50000.00"), StatusCompra.CANCELADA);

        mockMvc.perform(get("/cliente/me/compras?page=0&size=2").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.number").value(0))
                .andExpect(jsonPath("$.page.totalElements").value(3))
                .andExpect(jsonPath("$.page.totalPages").value(2));

        mockMvc.perform(get("/cliente/me/compras?page=1&size=2").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));
    }

    @Test
    void tamanhoDePaginaPadraoEMaximo() throws Exception {
        mockMvc.perform(get("/cliente/me/compras").header("Authorization", bearer(usuario)))
                .andExpect(jsonPath("$.page.size").value(20));

        mockMvc.perform(get("/cliente/me/compras?size=5000").header("Authorization", bearer(usuario)))
                .andExpect(jsonPath("$.page.size").value(100));
    }

    @Test
    void campoDeOrdenacaoInexistenteRetorna400() throws Exception {
        mockMvc.perform(get("/cliente/me/compras?sort=campoQueNaoExiste").header("Authorization", bearer(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Campo de ordenação inválido: campoQueNaoExiste"));
    }

    @Test
    void listaVaziaRetornaNormalmente() throws Exception {
        mockMvc.perform(get("/cliente/me/compras").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void clienteNaoConsultaComprasDeOutroCliente() throws Exception {
        Compra deOutro = criarCompra(outroCliente, carro, StatusCompra.PENDENTE);

        mockMvc.perform(get("/compra/" + deOutro.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/compra").header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administradorListaCompras() throws Exception {
        Compra minha = criarCompra(cliente, carro, StatusCompra.PENDENTE);
        Compra deOutro = criarCompra(outroCliente, criarCarro(StatusCarro.DISPONIVEL, "30000.00"), StatusCompra.PENDENTE);

        mockMvc.perform(get("/compra").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(minha.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(deOutro.getId())).exists());
    }

    @Test
    void administradorBuscaCompraPorId() throws Exception {
        Compra compra = criarCompra(cliente, carro, StatusCompra.PENDENTE);

        mockMvc.perform(get("/compra/" + compra.getId()).header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(compra.getId().toString()))
                .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()));

        mockMvc.perform(get("/compra/" + UUID.randomUUID()).header("Authorization", bearer(administrador)))
                .andExpect(status().isNotFound());
    }

    @Test
    void consultasSemAutenticacaoRetornam401() throws Exception {
        mockMvc.perform(get("/cliente/me/compras")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/compra")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/compra/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    @Test
    void naoExisteExclusaoDeCompra() throws Exception {
        Compra compra = criarCompra(cliente, carro, StatusCompra.PENDENTE);

        mockMvc.perform(delete("/compra/" + compra.getId()).header("Authorization", bearer(administrador)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(404, 405));

        assertThat(compraRepository.existsById(compra.getId())).isTrue();
    }

    private ResultActions comprar(Usuario autor, UUID carroId) throws Exception {
        return comprar(autor, "{\"carroId\":\"%s\"}".formatted(carroId));
    }

    private ResultActions comprar(Usuario autor, String body) throws Exception {
        return mockMvc.perform(post("/compra")
                .header("Authorization", bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String bearer(Usuario autor) {
        return "Bearer " + jwtService.gerarToken(autor);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private StatusCarro statusDoCarro(Carro alvo) {
        recarregarContexto();
        return carroRepository.findById(alvo.getId()).orElseThrow().getStatus();
    }

    private ResultActions aprovarPagamento(Pagamento pagamento) throws Exception {
        return mockMvc.perform(put("/pagamento/" + pagamento.getId() + "/aprovar").header("Authorization", bearer(administrador)));
    }

    private Pagamento criarPagamento(Compra alvo, StatusPagamento status) {
        Pagamento pagamento = new Pagamento();
        pagamento.setCompra(alvo);
        pagamento.setMetodo(MetodoPagamento.PIX);
        pagamento.setValor(alvo.getValorTotal());
        pagamento.setStatus(status);
        return pagamentoRepository.saveAndFlush(pagamento);
    }

    private Compra criarCompraPendenteComCarroReservado() {
        carro.setStatus(StatusCarro.RESERVADO);
        carroRepository.saveAndFlush(carro);
        return criarCompra(cliente, carro, StatusCompra.PENDENTE);
    }

    private Carro criarCarroComCompraAprovada() {
        Carro vendido = criarCarro(StatusCarro.VENDIDO, "70000.00");
        criarCompra(outroCliente, vendido, StatusCompra.APROVADA);
        return vendido;
    }

    private Compra criarCompra(Cliente dono, Carro alvo, StatusCompra status) {
        Compra compra = new Compra();
        compra.setCliente(dono);
        compra.setCarro(alvo);
        compra.setValorTotal(alvo.getPreco());
        compra.setStatus(status);
        return compraRepository.saveAndFlush(compra);
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
