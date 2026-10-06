package com.reginaldo.apisistemavendacarros.service;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.dto.usuario.UsuarioRequest;
import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.reginaldo.apisistemavendacarros.CsrfReal.tokenCsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// SMTP falso em memória (GreenMail) no lugar do Mailtrap.
// Sem @Transactional: o e-mail só sai após o commit, então os dados são apagados no @AfterEach
@SpringBootTest(properties = {
        "email.ativo=true",
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.username=",
        "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false"
})
@AutoConfigureMockMvc
@Import(DadosTeste.class)
class EmailServiceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private PagamentoService pagamentoService;

    @Autowired
    private CompraService compraService;

    @Autowired
    private DadosTeste fabrica;

    private final List<String> emailsCriados = new ArrayList<>();

    @AfterEach
    void limpar() {
        fabrica.limpar();
        emailsCriados.forEach(email -> usuarioRepository.findByEmail(email).ifPresent(usuarioRepository::delete));
    }

    @Test
    void cadastroEnviaEmailDeBoasVindasEmHtml() throws Exception {
        String email = novoEmail();

        cadastrar(email).andExpect(status().isCreated());

        // Envio é assíncrono: espera o e-mail chegar
        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

        MimeMessage mensagem = greenMail.getReceivedMessages()[0];
        assertThat(mensagem.getAllRecipients()[0].toString()).isEqualTo(email);
        assertThat(mensagem.getSubject()).isEqualTo("Bem-vindo ao Sistema de Venda de Carros");
        assertThat(mensagem.getContentType()).startsWith("text/html");
        assertThat((String) mensagem.getContent()).contains("Olá, <strong>Maria</strong>!");
    }

    @Test
    void cadastroComEmailDuplicadoNaoEnviaEmail() throws Exception {
        String email = novoEmail();

        cadastrar(email).andExpect(status().isCreated());
        greenMail.waitForIncomingEmail(5000, 1);

        cadastrar(email).andExpect(status().isConflict());

        assertThat(greenMail.waitForIncomingEmail(1000, 2)).isFalse();
        assertThat(greenMail.getReceivedMessages()).hasSize(1);
    }

    @Test
    void cadastroDesfeitoNaoEnviaEmail() {
        String email = novoEmail();

        // Simula falha depois do cadastro: a transação é desfeita, então não há commit
        transactionTemplate.executeWithoutResult(status -> {
            usuarioService.cadastro(new UsuarioRequest("Maria", email, "minhaSenha"));
            status.setRollbackOnly();
        });

        assertThat(usuarioRepository.existsByEmail(email)).isFalse();
        assertThat(greenMail.waitForIncomingEmail(1000, 1)).isFalse();
    }

    @Test
    void pagamentoAprovadoEnviaEmailDeCompraAprovada() throws Exception {
        Usuario usuario = fabrica.usuario(PerfilUsuario.USUARIO);
        Carro carro = fabrica.carro(StatusCarro.RESERVADO);
        Compra compra = fabrica.compra(fabrica.cliente(usuario), carro, StatusCompra.PENDENTE);
        Pagamento pagamento = fabrica.pagamento(compra, StatusPagamento.PENDENTE);

        pagamentoService.aprovar(pagamento.getId());

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

        MimeMessage mensagem = greenMail.getReceivedMessages()[0];
        assertThat(mensagem.getAllRecipients()[0].toString()).isEqualTo(usuario.getEmail());
        assertThat(mensagem.getSubject()).isEqualTo("Pagamento confirmado - " + carro.getNome());
        assertThat((String) mensagem.getContent()).contains(carro.getNome(), "Valor pago");
    }

    @Test
    void compraExpiradaEnviaEmailDeReservaExpirada() throws Exception {
        Usuario usuario = fabrica.usuario(PerfilUsuario.USUARIO);
        Carro carro = fabrica.carro(StatusCarro.RESERVADO);
        Compra compra = fabrica.compra(fabrica.cliente(usuario), carro, StatusCompra.PENDENTE);

        compraService.expirar(compra.getId());

        assertThat(greenMail.waitForIncomingEmail(5000, 1)).isTrue();

        MimeMessage mensagem = greenMail.getReceivedMessages()[0];
        assertThat(mensagem.getAllRecipients()[0].toString()).isEqualTo(usuario.getEmail());
        assertThat(mensagem.getSubject()).isEqualTo("Sua reserva expirou - " + carro.getNome());
        assertThat((String) mensagem.getContent()).contains(carro.getNome(), "Sua reserva expirou");
    }

    @Test
    void cancelamentoPeloClienteNaoEnviaEmailDeExpiracao() {
        Usuario usuario = fabrica.usuario(PerfilUsuario.USUARIO);
        Compra compra = fabrica.compra(fabrica.cliente(usuario), fabrica.carro(StatusCarro.RESERVADO), StatusCompra.PENDENTE);

        compraService.cancelar(compra.getId());

        assertThat(greenMail.waitForIncomingEmail(1000, 1)).isFalse();
    }

    private String novoEmail() {
        String email = UUID.randomUUID() + "@teste.com";
        emailsCriados.add(email);
        return email;
    }

    private ResultActions cadastrar(String email) throws Exception {
        return mockMvc.perform(post("/usuario").with(tokenCsrf(mockMvc))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nomeCompleto": "Maria", "email": "%s", "senha": "minhaSenha"}
                        """.formatted(email)));
    }
}
