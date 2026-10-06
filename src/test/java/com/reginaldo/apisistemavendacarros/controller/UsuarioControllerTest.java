package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.DadosTeste;
import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(DadosTeste.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DadosTeste fabrica;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FavoritoRepository favoritoRepository;

    @Autowired
    private InteresseCarroRepository interesseCarroRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario;
    private Usuario outroUsuario;
    private Usuario administrador;

    @BeforeEach
    void setUp() {
        usuario = fabrica.usuario(PerfilUsuario.USUARIO);
        outroUsuario = fabrica.usuario(PerfilUsuario.USUARIO);
        administrador = fabrica.usuario(PerfilUsuario.ADMINISTRADOR);
    }

    @Test
    void cadastroPublicoCriptografaSenha() throws Exception {
        String email = UUID.randomUUID() + "@teste.com";

        mockMvc.perform(post("/usuario").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", email, "minhaSenha")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("USUARIO"))
                .andExpect(jsonPath("$.senha").doesNotExist());

        Usuario salvo = usuarioRepository.findByEmail(email).orElseThrow();
        assertThat(salvo.getSenha()).isNotEqualTo("minhaSenha");
        assertThat(passwordEncoder.matches("minhaSenha", salvo.getSenha())).isTrue();
    }

    @Test
    void cadastroComEmailDuplicadoRetorna409() throws Exception {
        mockMvc.perform(post("/usuario").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", usuario.getEmail(), "minhaSenha")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    @Test
    void cadastroComEmailInvalidoOuSenhaGrandeRetorna400() throws Exception {
        mockMvc.perform(post("/usuario").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", "email-invalido", "a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.email").value("Email inválido"))
                .andExpect(jsonPath("$.mensagens.senha").exists());
    }

    @Test
    void cadastroComSenhaCurtaRetorna400() throws Exception {
        mockMvc.perform(post("/usuario").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", UUID.randomUUID() + "@teste.com", "1234567")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.senha").value("Senha deve ter entre 8 e 72 caracteres"));
    }

    @Test
    void loginBloqueadoDepoisDeCincoSenhasErradas() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(usuario.getEmail(), "senhaErrada").andExpect(status().isUnauthorized());
        }

        // Bloqueado mesmo com a senha certa, até a janela passar
        login(usuario.getEmail(), DadosTeste.SENHA)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.mensagem").value("Muitas tentativas de login com este e-mail. Aguarde alguns minutos e tente de novo."));

        // O bloqueio é só desse e-mail
        login(outroUsuario.getEmail(), DadosTeste.SENHA).andExpect(status().isOk());
    }

    @Test
    void loginCertoZeraAsFalhasAnteriores() throws Exception {
        for (int i = 0; i < 4; i++) {
            login(usuario.getEmail(), "senhaErrada").andExpect(status().isUnauthorized());
        }
        login(usuario.getEmail(), DadosTeste.SENHA).andExpect(status().isOk());

        login(usuario.getEmail(), "senhaErrada").andExpect(status().isUnauthorized());
        login(usuario.getEmail(), DadosTeste.SENHA).andExpect(status().isOk());
    }

    @Test
    void atualizarMeCriptografaSenhaEPermiteLoginComSenhaNova() throws Exception {
        String novoEmail = UUID.randomUUID() + "@teste.com";

        atualizarMe(usuario, atualizacao("Nome Novo", novoEmail, DadosTeste.SENHA, "senhaNova"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCompleto").value("Nome Novo"))
                .andExpect(jsonPath("$.email").value(novoEmail));

        recarregarContexto();
        Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("senhaNova", atualizado.getSenha())).isTrue();

        // Falha se a senha for gravada em texto puro
        login(novoEmail, "senhaNova").andExpect(status().isOk());
    }

    @Test
    void atualizarMeSoONomeNaoPedeSenha() throws Exception {
        atualizarMe(usuario, atualizacao("Nome Novo", usuario.getEmail(), null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCompleto").value("Nome Novo"));

        recarregarContexto();
        Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches(DadosTeste.SENHA, atualizado.getSenha())).isTrue();
    }

    @Test
    void trocarSenhaOuEmailSemASenhaAtualCertaRetorna400() throws Exception {
        atualizarMe(usuario, atualizacao("Nome", usuario.getEmail(), null, "senhaNova1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.senhaAtual").value("Senha atual incorreta"));

        atualizarMe(usuario, atualizacao("Nome", UUID.randomUUID() + "@teste.com", "senhaErrada", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.senhaAtual").value("Senha atual incorreta"));

        recarregarContexto();
        Usuario semMudanca = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(semMudanca.getEmail()).isEqualTo(usuario.getEmail());
        assertThat(passwordEncoder.matches(DadosTeste.SENHA, semMudanca.getSenha())).isTrue();
    }

    @Test
    void senhaAtualErradaCincoVezesBloqueiaAsTentativas() throws Exception {
        for (int i = 0; i < 5; i++) {
            atualizarMe(usuario, atualizacao("Nome", usuario.getEmail(), "senhaErrada", "senhaNova1"))
                    .andExpect(status().isBadRequest());
        }

        atualizarMe(usuario, atualizacao("Nome", usuario.getEmail(), DadosTeste.SENHA, "senhaNova1"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void novaSenhaCurtaRetorna400() throws Exception {
        atualizarMe(usuario, atualizacao("Nome", usuario.getEmail(), DadosTeste.SENHA, "curta"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.novaSenha").exists());
    }

    @Test
    void contaGoogleSemSenhaNaoCriaSenhaPorAqui() throws Exception {
        usuario.setProvedor(ProvedorAutenticacao.GOOGLE);
        usuario.setSenha(null);
        usuarioRepository.saveAndFlush(usuario);

        atualizarMe(usuario, atualizacao("Nome", usuario.getEmail(), null, "senhaNova1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void atualizarMeNaoAlteraPerfilNemProvedor() throws Exception {
        String body = """
                {"nomeCompleto":"X","email":"%s","perfil":"ADMINISTRADOR","provedor":"GOOGLE"}
                """.formatted(usuario.getEmail());

        atualizarMe(usuario, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("USUARIO"))
                .andExpect(jsonPath("$.provedor").value("LOCAL"));
    }

    @Test
    void atualizarMeComEmailDeOutroUsuarioRetorna409() throws Exception {
        atualizarMe(usuario, atualizacao("Nome", outroUsuario.getEmail(), DadosTeste.SENHA, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    @Test
    void usuarioDoGoogleNaoPodeTrocarEmail() throws Exception {
        usuario.setProvedor(ProvedorAutenticacao.GOOGLE);
        usuarioRepository.saveAndFlush(usuario);

        atualizarMe(usuario, atualizacao("Nome", UUID.randomUUID() + "@teste.com", DadosTeste.SENHA, null))
                .andExpect(status().isBadRequest());

        atualizarMe(usuario, atualizacao("Nome Novo", usuario.getEmail(), null, null))
                .andExpect(status().isOk());
    }

    @Test
    void excluirMeSemCliente() throws Exception {
        mockMvc.perform(delete("/usuario/me").header("Authorization", fabrica.bearer(usuario)))
                .andExpect(status().isNoContent());

        assertThat(usuarioRepository.existsById(usuario.getId())).isFalse();
    }

    @Test
    void excluirMeRemoveClienteEDependentesSemHistorico() throws Exception {
        Cliente cliente = fabrica.cliente(usuario);
        Carro carro = fabrica.carro(StatusCarro.DISPONIVEL);
        fabrica.favorito(cliente, carro);
        fabrica.interesse(cliente, carro);
        fabrica.endereco(cliente);

        mockMvc.perform(delete("/usuario/me").header("Authorization", fabrica.bearer(usuario)))
                .andExpect(status().isNoContent());

        recarregarContexto();
        assertThat(usuarioRepository.existsById(usuario.getId())).isFalse();
        assertThat(clienteRepository.existsById(cliente.getId())).isFalse();
        assertThat(favoritoRepository.findByClienteId(cliente.getId())).isEmpty();
        assertThat(interesseCarroRepository.findByClienteId(cliente.getId())).isEmpty();
        assertThat(enderecoRepository.findAllByClienteId(cliente.getId())).isEmpty();
    }

    @Test
    void excluirMeComComprasRetorna400ENaoApagaNada() throws Exception {
        Cliente cliente = fabrica.cliente(usuario);
        Carro carro = fabrica.carro(StatusCarro.RESERVADO);
        fabrica.compra(cliente, carro, StatusCompra.PENDENTE);
        fabrica.favorito(cliente, fabrica.carro(StatusCarro.DISPONIVEL));

        mockMvc.perform(delete("/usuario/me").header("Authorization", fabrica.bearer(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Cliente possui compras registradas e não pode ser excluído"));

        assertThat(usuarioRepository.existsById(usuario.getId())).isTrue();
        assertThat(clienteRepository.existsById(cliente.getId())).isTrue();
        assertThat(favoritoRepository.findByClienteId(cliente.getId())).hasSize(1);
    }

    @Test
    void usuarioComumNaoAcessaUsuariosDeOutros() throws Exception {
        String token = fabrica.bearer(usuario);

        mockMvc.perform(get("/usuario?sort=dataCriacao,desc").header("Authorization", token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensagem").value("Acesso negado"));

        mockMvc.perform(get("/usuario/" + outroUsuario.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/usuario/" + outroUsuario.getId()).header("Authorization", token))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepository.existsById(outroUsuario.getId())).isTrue();
    }

    @Test
    void naoExisteMaisPutPorIdParaUsuario() throws Exception {
        mockMvc.perform(put("/usuario/" + outroUsuario.getId())
                        .header("Authorization", fabrica.bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Hacker", outroUsuario.getEmail(), "trocada")))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(403, 404, 405));

        recarregarContexto();
        assertThat(usuarioRepository.findById(outroUsuario.getId()).orElseThrow().getNomeCompleto())
                .isNotEqualTo("Hacker");
    }

    @Test
    void administradorListaBuscaEExcluiUsuarios() throws Exception {
        String token = fabrica.bearer(administrador);

        mockMvc.perform(get("/usuario?sort=dataCriacao,desc").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(usuario.getId())).exists());

        mockMvc.perform(get("/usuario/" + usuario.getId()).header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(usuario.getEmail()));

        mockMvc.perform(delete("/usuario/" + usuario.getId()).header("Authorization", token))
                .andExpect(status().isNoContent());

        assertThat(usuarioRepository.existsById(usuario.getId())).isFalse();
    }

    @Test
    void semAutenticacaoRetorna401ComCorpo() throws Exception {
        mockMvc.perform(get("/usuario/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensagem").value("Autenticação necessária"));
    }

    private ResultActions atualizarMe(Usuario autor, String body) throws Exception {
        return mockMvc.perform(put("/usuario/me")
                .header("Authorization", fabrica.bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mockMvc.perform(post("/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }

    // null vira ausente no JSON, como o front manda quando o campo fica vazio
    private String atualizacao(String nome, String email, String senhaAtual, String novaSenha) {
        return "{\"nomeCompleto\":\"%s\",\"email\":\"%s\"%s%s}".formatted(nome, email,
                senhaAtual == null ? "" : ",\"senhaAtual\":\"%s\"".formatted(senhaAtual),
                novaSenha == null ? "" : ",\"novaSenha\":\"%s\"".formatted(novaSenha));
    }

    private String json(String nome, String email, String senha) {
        return "{\"nomeCompleto\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(nome, email, senha);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }
}
