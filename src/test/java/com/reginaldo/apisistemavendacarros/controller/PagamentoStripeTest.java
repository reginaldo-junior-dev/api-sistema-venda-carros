package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.ApiConnectionException;
import com.stripe.exception.CardException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeError;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// StripeClient mockado: nenhuma chamada sai para a Stripe
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "stripe.webhook-secret=" + PagamentoStripeTest.WEBHOOK_SECRET)
class PagamentoStripeTest {

    static final String WEBHOOK_SECRET = "whsec_teste";

    @MockitoBean(answers = Answers.RETURNS_DEEP_STUBS)
    private StripeClient stripeClient;

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
    private Usuario administrador;
    private Carro carro;
    private Compra compra;

    @BeforeEach
    void setUp() {
        usuario = criarUsuario(PerfilUsuario.USUARIO);
        administrador = criarUsuario(PerfilUsuario.ADMINISTRADOR);
        Cliente cliente = criarCliente(usuario);
        carro = criarCarro();
        compra = criarCompra(cliente, carro);
    }

    @Test
    void cartaoAprovadoAprovaCompraEVendeCarro() throws Exception {
        quandoCriarPaymentIntentRetornar(paymentIntent("pi_aprovado", "succeeded"));

        pagarComCartao("pm_card_visa")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.idExterno").value("pi_aprovado"))
                .andExpect(jsonPath("$.dataPagamento").exists())
                .andExpect(jsonPath("$.clientSecret").isEmpty());

        assertThat(statusDaCompra()).isEqualTo(StatusCompra.APROVADA);
        assertThat(statusDoCarro()).isEqualTo(StatusCarro.VENDIDO);
    }

    @Test
    void valorEnviadoEmCentavosComIdDoPagamentoComoChaveDeIdempotencia() throws Exception {
        quandoCriarPaymentIntentRetornar(paymentIntent("pi_aprovado", "succeeded"));

        pagarComCartao("pm_card_visa").andExpect(status().isCreated());

        var params = ArgumentCaptor.forClass(PaymentIntentCreateParams.class);
        var opcoes = ArgumentCaptor.forClass(RequestOptions.class);
        verify(stripeClient.v1().paymentIntents()).create(params.capture(), opcoes.capture());

        UUID pagamentoId = pagamentoRepository.findByCompraClienteId(compra.getCliente().getId()).getFirst().getId();
        assertThat(params.getValue().getAmount()).isEqualTo(8_500_000L);
        assertThat(params.getValue().getCurrency()).isEqualTo("brl");
        assertThat(params.getValue().getPaymentMethod()).isEqualTo("pm_card_visa");
        assertThat(opcoes.getValue().getIdempotencyKey()).isEqualTo(pagamentoId.toString());
    }

    @Test
    void cartaoRecusadoGravaPagamentoRecusadoEMantemCompraAberta() throws Exception {
        PaymentIntent recusado = paymentIntent("pi_recusado", "requires_payment_method");
        recusado.setLastPaymentError(new StripeError());
        StripeError erro = new StripeError();
        erro.setPaymentIntent(recusado);
        CardException cartaoRecusado = new CardException("Your card was declined.", "req_1", "card_declined",
                null, "generic_decline", null, 402, null);
        cartaoRecusado.setStripeError(erro);
        when(stripeClient.v1().paymentIntents().create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                .thenThrow(cartaoRecusado);

        pagarComCartao("pm_card_chargeDeclined")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.idExterno").value("pi_recusado"));

        assertThat(statusDaCompra()).isEqualTo(StatusCompra.PENDENTE);
        assertThat(statusDoCarro()).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void tresDSecureFicaPendenteEDevolveClientSecret() throws Exception {
        PaymentIntent aguardando = paymentIntent("pi_3ds", "requires_action");
        aguardando.setClientSecret("pi_3ds_secret_abc");
        quandoCriarPaymentIntentRetornar(aguardando);

        pagarComCartao("pm_card_authenticationRequired")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.clientSecret").value("pi_3ds_secret_abc"));

        assertThat(statusDaCompra()).isEqualTo(StatusCompra.PENDENTE);
    }

    // O rollback não é verificado aqui: o @Transactional do teste só desfaz no final
    @Test
    void falhaDeComunicacaoComAStripeRetorna502() throws Exception {
        when(stripeClient.v1().paymentIntents().create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                .thenThrow(new ApiConnectionException("timeout"));

        pagarComCartao("pm_card_visa").andExpect(status().isBadGateway());
    }

    @Test
    void novaTentativaCancelaPendenteNaStripeESegue() throws Exception {
        Pagamento abandonado = criarPagamentoStripe("pi_abandonado", StatusPagamento.PENDENTE);
        when(stripeClient.v1().paymentIntents().retrieve("pi_abandonado"))
                .thenReturn(paymentIntent("pi_abandonado", "requires_action"));
        quandoCriarPaymentIntentRetornar(paymentIntent("pi_novo", "succeeded"));

        pagarComCartao("pm_card_visa")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"));

        verify(stripeClient.v1().paymentIntents()).cancel("pi_abandonado");
        assertThat(statusDoPagamento(abandonado)).isEqualTo(StatusPagamento.CANCELADO);
    }

    @Test
    void novaTentativaBloqueadaQuandoStripeJaEstaProcessando() throws Exception {
        Pagamento emProcessamento = criarPagamentoStripe("pi_processando", StatusPagamento.PENDENTE);
        when(stripeClient.v1().paymentIntents().retrieve("pi_processando"))
                .thenReturn(paymentIntent("pi_processando", "processing"));

        pagarComCartao("pm_card_visa").andExpect(status().isConflict());

        verify(stripeClient.v1().paymentIntents(), never()).cancel(anyString());
        verify(stripeClient.v1().paymentIntents(), never())
                .create(any(PaymentIntentCreateParams.class), any(RequestOptions.class));
        assertThat(statusDoPagamento(emProcessamento)).isEqualTo(StatusPagamento.PENDENTE);
    }

    @Test
    void administradorNaoAprovaPagamentoDaStripeManualmente() throws Exception {
        Pagamento pagamento = criarPagamentoStripe("pi_3ds", StatusPagamento.PENDENTE);

        acao(pagamento, "aprovar").andExpect(status().isBadRequest());

        assertThat(statusDoPagamento(pagamento)).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(statusDaCompra()).isEqualTo(StatusCompra.PENDENTE);
        assertThat(statusDoCarro()).isEqualTo(StatusCarro.RESERVADO);
    }

    @Test
    void administradorCancelaPagamentoDaStripeCancelandoLaTambem() throws Exception {
        Pagamento pagamento = criarPagamentoStripe("pi_3ds", StatusPagamento.PENDENTE);
        when(stripeClient.v1().paymentIntents().retrieve("pi_3ds"))
                .thenReturn(paymentIntent("pi_3ds", "requires_action"));

        acao(pagamento, "cancelar")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));

        verify(stripeClient.v1().paymentIntents()).cancel("pi_3ds");
    }

    @Test
    void cancelarCompraCancelaPagamentoPendenteNaStripe() throws Exception {
        Pagamento pagamento = criarPagamentoStripe("pi_3ds", StatusPagamento.PENDENTE);
        when(stripeClient.v1().paymentIntents().retrieve("pi_3ds"))
                .thenReturn(paymentIntent("pi_3ds", "requires_action"));

        mockMvc.perform(put("/compra/" + compra.getId() + "/cancelar").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk());

        verify(stripeClient.v1().paymentIntents()).cancel("pi_3ds");
        assertThat(statusDoPagamento(pagamento)).isEqualTo(StatusPagamento.CANCELADO);
        assertThat(statusDoCarro()).isEqualTo(StatusCarro.DISPONIVEL);
    }

    @Test
    void webhookSemAssinaturaOuComAssinaturaInvalidaRetorna401() throws Exception {
        String payload = evento("payment_intent.succeeded", "pi_qualquer");

        mockMvc.perform(post("/stripe/webhook").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1,v1=assinatura_falsa")
                        .content(payload))
                .andExpect(status().isUnauthorized());

        verify(stripeClient.v1().paymentIntents(), never()).retrieve(anyString());
    }

    @Test
    void webhookPagamentoConcluidoAprovaPagamentoPendente() throws Exception {
        Pagamento pagamento = criarPagamentoStripe("pi_3ds", StatusPagamento.PENDENTE);
        when(stripeClient.v1().paymentIntents().retrieve("pi_3ds"))
                .thenReturn(paymentIntent("pi_3ds", "succeeded"));

        enviarWebhook(evento("payment_intent.succeeded", "pi_3ds")).andExpect(status().isOk());

        assertThat(statusDoPagamento(pagamento)).isEqualTo(StatusPagamento.APROVADO);
        assertThat(statusDaCompra()).isEqualTo(StatusCompra.APROVADA);
        assertThat(statusDoCarro()).isEqualTo(StatusCarro.VENDIDO);
    }

    @Test
    void webhookPagamentoFalhouRecusaPagamentoPendente() throws Exception {
        Pagamento pagamento = criarPagamentoStripe("pi_3ds", StatusPagamento.PENDENTE);
        PaymentIntent falhou = paymentIntent("pi_3ds", "requires_payment_method");
        falhou.setLastPaymentError(new StripeError());
        when(stripeClient.v1().paymentIntents().retrieve("pi_3ds")).thenReturn(falhou);

        enviarWebhook(evento("payment_intent.payment_failed", "pi_3ds")).andExpect(status().isOk());

        assertThat(statusDoPagamento(pagamento)).isEqualTo(StatusPagamento.RECUSADO);
        assertThat(statusDaCompra()).isEqualTo(StatusCompra.PENDENTE);
    }

    @Test
    void webhookRepetidoNaoAlteraPagamentoJaProcessado() throws Exception {
        Pagamento pagamento = criarPagamentoStripe("pi_recusado", StatusPagamento.RECUSADO);
        PaymentIntent falhou = paymentIntent("pi_recusado", "requires_payment_method");
        falhou.setLastPaymentError(new StripeError());
        when(stripeClient.v1().paymentIntents().retrieve("pi_recusado")).thenReturn(falhou);

        enviarWebhook(evento("payment_intent.payment_failed", "pi_recusado")).andExpect(status().isOk());

        assertThat(statusDoPagamento(pagamento)).isEqualTo(StatusPagamento.RECUSADO);
    }

    @Test
    void webhookDeEventoNaoTratadoEIgnorado() throws Exception {
        enviarWebhook(evento("charge.succeeded", "ch_qualquer")).andExpect(status().isOk());

        verify(stripeClient.v1().paymentIntents(), never()).retrieve(anyString());
    }

    private void quandoCriarPaymentIntentRetornar(PaymentIntent resposta) throws Exception {
        when(stripeClient.v1().paymentIntents().create(any(PaymentIntentCreateParams.class), any(RequestOptions.class)))
                .thenReturn(resposta);
    }

    private PaymentIntent paymentIntent(String id, String status) {
        PaymentIntent paymentIntent = new PaymentIntent();
        paymentIntent.setId(id);
        paymentIntent.setStatus(status);
        return paymentIntent;
    }

    private String evento(String tipo, String objetoId) {
        String objeto = tipo.startsWith("payment_intent")
                ? "{\"id\":\"%s\",\"object\":\"payment_intent\"}".formatted(objetoId)
                : "{\"id\":\"%s\",\"object\":\"charge\"}".formatted(objetoId);

        return """
                {"id":"evt_%s","object":"event","api_version":"%s","type":"%s","data":{"object":%s}}
                """.formatted(UUID.randomUUID(), Stripe.API_VERSION, tipo, objeto).trim();
    }

    // Assina como a Stripe: HMAC-SHA256 de "<timestamp>.<corpo>"
    private ResultActions enviarWebhook(String payload) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String assinatura = Webhook.Util.computeHmacSha256(WEBHOOK_SECRET, timestamp + "." + payload);

        return mockMvc.perform(post("/stripe/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Stripe-Signature", "t=" + timestamp + ",v1=" + assinatura)
                .content(payload));
    }

    private ResultActions pagarComCartao(String paymentMethodId) throws Exception {
        return mockMvc.perform(post("/pagamento/cartao")
                .header("Authorization", bearer(usuario))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"compraId\":\"%s\",\"paymentMethodId\":\"%s\"}".formatted(compra.getId(), paymentMethodId)));
    }

    private ResultActions acao(Pagamento pagamento, String acao) throws Exception {
        return mockMvc.perform(put("/pagamento/" + pagamento.getId() + "/" + acao)
                .header("Authorization", bearer(administrador)));
    }

    private String bearer(Usuario autor) {
        return "Bearer " + jwtService.gerarToken(autor);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private StatusPagamento statusDoPagamento(Pagamento pagamento) {
        recarregarContexto();
        return pagamentoRepository.findById(pagamento.getId()).orElseThrow().getStatus();
    }

    private StatusCompra statusDaCompra() {
        recarregarContexto();
        return compraRepository.findById(compra.getId()).orElseThrow().getStatus();
    }

    private StatusCarro statusDoCarro() {
        recarregarContexto();
        return carroRepository.findById(carro.getId()).orElseThrow().getStatus();
    }

    private Pagamento criarPagamentoStripe(String paymentIntentId, StatusPagamento status) {
        Pagamento pagamento = new Pagamento();
        pagamento.setCompra(compra);
        pagamento.setMetodo(MetodoPagamento.CARTAO_CREDITO);
        pagamento.setValor(compra.getValorTotal());
        pagamento.setStatus(status);
        pagamento.setIdExterno(paymentIntentId);
        return pagamentoRepository.saveAndFlush(pagamento);
    }

    private Compra criarCompra(Cliente dono, Carro alvo) {
        Compra nova = new Compra();
        nova.setCliente(dono);
        nova.setCarro(alvo);
        nova.setValorTotal(alvo.getPreco());
        nova.setStatus(StatusCompra.PENDENTE);
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

    private Cliente criarCliente(Usuario dono) {
        Cliente novo = new Cliente();
        novo.setUsuario(dono);
        novo.setCpf("12345678901");
        novo.setDataNascimento(LocalDate.of(1990, 5, 10));
        novo.setTelefone("11987654321");
        return clienteRepository.saveAndFlush(novo);
    }

    private Carro criarCarro() {
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
        novo.setPreco(new BigDecimal("85000.00"));
        novo.setDescricao("Carro de teste");
        novo.setAnoFabricacao(2020);
        novo.setAnoModelo(2021);
        novo.setQuilometragem(10000);
        novo.setCondicao(CondicaoCarro.USADO);
        novo.setCombustivel(TipoCombustivel.FLEX);
        novo.setCambio(TipoCambio.MANUAL);
        novo.setStatus(StatusCarro.RESERVADO);
        novo.setModelo(modelo);
        novo.setCor(cor);
        novo.setCategoria(categoria);
        return carroRepository.saveAndFlush(novo);
    }
}
