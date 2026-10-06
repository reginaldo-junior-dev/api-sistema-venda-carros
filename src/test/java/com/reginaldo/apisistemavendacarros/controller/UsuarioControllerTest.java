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

        mockMvc.perform(post("/usuario")
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
        mockMvc.perform(post("/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", usuario.getEmail(), "minhaSenha")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    @Test
    void cadastroComEmailInvalidoOuSenhaGrandeRetorna400() throws Exception {
        mockMvc.perform(post("/usuario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", "email-invalido", "a".repeat(73))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.email").value("Email inválido"))
                .andExpect(jsonPath("$.mensagens.senha").exists());
    }

    @Test
    void atualizarMeCriptografaSenhaEPermiteLoginComSenhaNova() throws Exception {
        String novoEmail = UUID.randomUUID() + "@teste.com";

        atualizarMe(usuario, json("Nome Novo", novoEmail, "senhaNova"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeCompleto").value("Nome Novo"))
                .andExpect(jsonPath("$.email").value(novoEmail));

        recarregarContexto();
        Usuario atualizado = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("senhaNova", atualizado.getSenha())).isTrue();

        // Falha se a senha for gravada em texto puro
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"senhaNova\"}".formatted(novoEmail)))
                .andExpect(status().isOk());
    }

    @Test
    void atualizarMeNaoAlteraPerfilNemProvedor() throws Exception {
        String body = """
                {"nomeCompleto":"X","email":"%s","senha":"abc123","perfil":"ADMINISTRADOR","provedor":"GOOGLE"}
                """.formatted(usuario.getEmail());

        atualizarMe(usuario, body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("USUARIO"))
                .andExpect(jsonPath("$.provedor").value("LOCAL"));
    }

    @Test
    void atualizarMeComEmailDeOutroUsuarioRetorna409() throws Exception {
        atualizarMe(usuario, json("Nome", outroUsuario.getEmail(), "senha123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("E-mail já cadastrado"));
    }

    @Test
    void usuarioDoGoogleNaoPodeTrocarEmail() throws Exception {
        usuario.setProvedor(ProvedorAutenticacao.GOOGLE);
        usuarioRepository.saveAndFlush(usuario);

        atualizarMe(usuario, json("Nome", UUID.randomUUID() + "@teste.com", "senha123"))
                .andExpect(status().isBadRequest());

        atualizarMe(usuario, json("Nome Novo", usuario.getEmail(), "senha123"))
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

    private String json(String nome, String email, String senha) {
        return "{\"nomeCompleto\":\"%s\",\"email\":\"%s\",\"senha\":\"%s\"}".formatted(nome, email, senha);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }
}
