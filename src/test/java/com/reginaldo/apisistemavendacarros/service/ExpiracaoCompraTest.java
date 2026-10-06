package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import com.stripe.StripeClient;
import com.stripe.model.PaymentIntent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

// O agendamento fica desligado nos testes; o job é chamado direto
@SpringBootTest
@Transactional
class ExpiracaoCompraTest {

    private static final LocalDateTime ANTIGA = LocalDateTime.now().minusHours(2);

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private StripeClient stripeClient;

    @Autowired
    private ExpiracaoCompraService expiracaoCompraService;

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
    private EntityManager entityManager;

    private Cliente cliente;

    @BeforeEach
    void setUp() {
        Usuario usuario = new Usuario();
        usuario.setNomeCompleto("Teste expiração");
        usuario.setEmail(UUID.randomUUID() + "@teste.com");
        usuario.setPerfil(PerfilUsuario.USUARIO);
        usuario.setProvedor(ProvedorAutenticacao.LOCAL);
        usuarioRepository.saveAndFlush(usuario);

        cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setCpf("12345678901");
        cliente.setDataNascimento(LocalDate.of(1990, 5, 10));
        cliente.setTelefone("11987654321");
        clienteRepository.saveAndFlush(cliente);
    }

    @Test
    void compraPendenteAntigaECanceladaELiberaOCarro() {
        Compra compra = criarCompraPendente(ANTIGA);

        expiracaoCompraService.expirarCompras();

        assertThat(statusDaCompra(compra)).isEqualTo(StatusCompra.CANCELADA);
        assertThat(statusDoCarro(compra)).isEqualTo(StatusCarro.DISPONIVEL);
    }

    @Test
    void compraPendenteRecenteNaoExpira() {
        Compra compra = criarCompraPendente(LocalDateTime.now().minusMinutes(5));

        expiracaoCompraService.expirarCompras();

        assertThat(statusDaCompra(compra)).isEqualTo(StatusCompra.PENDENTE);
        assertThat(statusDoCarro(compra)).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void compraComPagamentoManualPendenteNaoExpira() {
        Compra compra = criarCompraPendente(ANTIGA);
        criarPagamento(compra, null);

        expiracaoCompraService.expirarCompras();

        assertThat(statusDaCompra(compra)).isEqualTo(StatusCompra.PENDENTE);
    }

    @Test
    void compraComPagamentoDaStripeAbandonadoExpiraECancelaNaStripe() throws Exception {
        Compra compra = criarCompraPendente(ANTIGA);
        Pagamento pagamento = criarPagamento(compra, "pi_abandonado");
        when(stripeClient.v1().paymentIntents().retrieve("pi_abandonado"))
                .thenReturn(paymentIntent("pi_abandonado", "requires_action"));

        expiracaoCompraService.expirarCompras();

        verify(stripeClient.v1().paymentIntents()).cancel("pi_abandonado");
        assertThat(statusDaCompra(compra)).isEqualTo(StatusCompra.CANCELADA);
        assertThat(pagamentoRepository.findById(pagamento.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusPagamento.CANCELADO);
        assertThat(statusDoCarro(compra)).isEqualTo(StatusCarro.DISPONIVEL);
    }

    @Test
    void compraComPagamentoProcessandoNaStripeNaoExpira() throws Exception {
        Compra compra = criarCompraPendente(ANTIGA);
        criarPagamento(compra, "pi_processando");
        when(stripeClient.v1().paymentIntents().retrieve("pi_processando"))
                .thenReturn(paymentIntent("pi_processando", "processing"));

        expiracaoCompraService.expirarCompras();

        verify(stripeClient.v1().paymentIntents(), never()).cancel(anyString());
        assertThat(statusDaCompra(compra)).isEqualTo(StatusCompra.PENDENTE);
        assertThat(statusDoCarro(compra)).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void compraAprovadaAntigaNaoEAlterada() {
        Compra compra = criarCompraPendente(ANTIGA);
        compra.setStatus(StatusCompra.APROVADA);
        compra.getCarro().setStatus(StatusCarro.VENDIDO);
        compraRepository.saveAndFlush(compra);

        expiracaoCompraService.expirarCompras();

        assertThat(statusDaCompra(compra)).isEqualTo(StatusCompra.APROVADA);
        assertThat(statusDoCarro(compra)).isEqualTo(StatusCarro.VENDIDO);
    }

    private PaymentIntent paymentIntent(String id, String status) {
        PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId(id);
        paymentIntent.setStatus(status);
        return paymentIntent;
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private StatusCompra statusDaCompra(Compra compra) {
        recarregarContexto();
        return compraRepository.findById(compra.getId()).orElseThrow().getStatus();
    }

    private StatusCarro statusDoCarro(Compra compra) {
        recarregarContexto();
        return carroRepository.findById(compra.getCarro().getId()).orElseThrow().getStatus();
    }

    private Pagamento criarPagamento(Compra compra, String idExterno) {
        Pagamento pagamento = new Pagamento();
        pagamento.setCompra(compra);
        pagamento.setMetodo(idExterno == null ? MetodoPagamento.PIX : MetodoPagamento.CARTAO_CREDITO);
        pagamento.setValor(compra.getValorTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento.setIdExterno(idExterno);
        return pagamentoRepository.saveAndFlush(pagamento);
    }

    // dataCompra é preenchida no @PrePersist; aqui é sobrescrita para simular uma compra antiga
    private Compra criarCompraPendente(LocalDateTime dataCompra) {
        Compra compra = new Compra();
        compra.setCliente(cliente);
        compra.setCarro(criarCarroReservado());
        compra.setValorTotal(compra.getCarro().getPreco());
        compra.setStatus(StatusCompra.PENDENTE);
        compraRepository.saveAndFlush(compra);

        compra.setDataCompra(dataCompra);
        return compraRepository.saveAndFlush(compra);
    }

    private Carro criarCarroReservado() {
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

        Carro carro = new Carro();
        carro.setNome("Carro " + sufixo);
        carro.setPreco(new BigDecimal("85000.00"));
        carro.setDescricao("Carro de teste");
        carro.setAnoFabricacao(2020);
        carro.setAnoModelo(2021);
        carro.setQuilometragem(10000);
        carro.setCondicao(CondicaoCarro.USADO);
        carro.setCombustivel(TipoCombustivel.FLEX);
        carro.setCambio(TipoCambio.MANUAL);
        carro.setStatus(StatusCarro.RESERVADO);
        carro.setModelo(modelo);
        carro.setCor(cor);
        carro.setCategoria(categoria);
        return carroRepository.saveAndFlush(carro);
    }
}
