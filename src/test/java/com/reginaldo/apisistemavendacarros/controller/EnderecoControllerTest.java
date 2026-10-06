package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Endereco;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.EnderecoRepository;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
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
class EnderecoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EnderecoRepository enderecoRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    private Usuario usuario;
    private Usuario outroUsuario;
    private Usuario administrador;
    private Cliente cliente;
    private Cliente outroCliente;

    @BeforeEach
    void setUp() {
        usuario = criarUsuario(PerfilUsuario.USUARIO);
        outroUsuario = criarUsuario(PerfilUsuario.USUARIO);
        administrador = criarUsuario(PerfilUsuario.ADMINISTRADOR);
        cliente = criarCliente(usuario, "12345678901");
        outroCliente = criarCliente(outroUsuario, "98765432100");
    }

    @Test
    void primeiroEnderecoViraPrincipalAutomaticamente() throws Exception {
        cadastrar(usuario, json("01310100", "SP", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principal").value(true))
                .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()));
    }

    @Test
    void clienteIdEnviadoNoCorpoEhIgnorado() throws Exception {
        String body = json("01310100", "SP", false).replace("}", ",\"clienteId\":\"" + outroCliente.getId() + "\"}");

        cadastrar(usuario, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()));

        assertThat(enderecoRepository.existsByClienteId(outroCliente.getId())).isFalse();
    }

    @Test
    void novoEnderecoPrincipalDesmarcaAnterior() throws Exception {
        Endereco antigo = criarEndereco(cliente, true);

        cadastrar(usuario, json("20040020", "RJ", true))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principal").value(true));

        recarregarContexto();
        assertThat(enderecoRepository.findById(antigo.getId()).orElseThrow().getPrincipal()).isFalse();
    }

    @Test
    void novoEnderecoNaoPrincipalMantemAnterior() throws Exception {
        Endereco antigo = criarEndereco(cliente, true);

        cadastrar(usuario, json("20040020", "RJ", false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.principal").value(false));

        recarregarContexto();
        assertThat(enderecoRepository.findById(antigo.getId()).orElseThrow().getPrincipal()).isTrue();
    }

    @Test
    void cadastroSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(post("/endereco").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("01310100", "SP", true)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioSemClienteRetorna404() throws Exception {
        Usuario semCliente = criarUsuario(PerfilUsuario.USUARIO);

        cadastrar(semCliente, json("01310100", "SP", true))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado"));
    }

    @Test
    void cepInvalidoRetornaErro() throws Exception {
        cadastrar(usuario, json("0131010", "SP", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.cep").exists());

        cadastrar(usuario, json("01310-100", "SP", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.cep").exists());
    }

    @Test
    void estadoInvalidoRetornaErro() throws Exception {
        cadastrar(usuario, json("01310100", "sp", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.estado").exists());

        cadastrar(usuario, json("01310100", "SPA", true))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.estado").exists());
    }

    @Test
    void principalNuloRetornaErro() throws Exception {
        cadastrar(usuario, json("01310100", "SP", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.principal").exists());
    }

    @Test
    void numeroMaiorQueColunaRetornaErro() throws Exception {
        String body = json("01310100", "SP", true).replace("\"numero\":\"100\"", "\"numero\":\"12345678901\"");

        cadastrar(usuario, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.numero").exists());
    }

    @Test
    void usuarioListaSomenteSeusEnderecos() throws Exception {
        criarEndereco(cliente, true);
        criarEndereco(cliente, false);
        criarEndereco(outroCliente, true);

        mockMvc.perform(get("/endereco").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].clienteId").value(cliente.getId().toString()))
                .andExpect(jsonPath("$[1].clienteId").value(cliente.getId().toString()));
    }

    @Test
    void usuarioNaoAcessaEnderecoDeOutro() throws Exception {
        Endereco enderecoOutro = criarEndereco(outroCliente, true);

        mockMvc.perform(get("/endereco/" + enderecoOutro.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/endereco/" + enderecoOutro.getId())
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("20040020", "RJ", true)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/endereco/" + enderecoOutro.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isNotFound());

        recarregarContexto();
        Endereco intacto = enderecoRepository.findById(enderecoOutro.getId()).orElseThrow();
        assertThat(intacto.getCep()).isEqualTo("01310100");
    }

    @Test
    void usuarioConsultaSeuEndereco() throws Exception {
        Endereco endereco = criarEndereco(cliente, true);

        mockMvc.perform(get("/endereco/" + endereco.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(endereco.getId().toString()));
    }

    @Test
    void usuarioAtualizaSeuEndereco() throws Exception {
        Endereco endereco = criarEndereco(cliente, true);

        mockMvc.perform(put("/endereco/" + endereco.getId())
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("20040020", "RJ", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("20040020"))
                .andExpect(jsonPath("$.estado").value("RJ"))
                .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()));
    }

    @Test
    void marcarOutroComoPrincipalNaAtualizacaoDesmarcaAnterior() throws Exception {
        Endereco principal = criarEndereco(cliente, true);
        Endereco secundario = criarEndereco(cliente, false);

        mockMvc.perform(put("/endereco/" + secundario.getId())
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("20040020", "RJ", true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principal").value(true));

        recarregarContexto();
        assertThat(enderecoRepository.findById(principal.getId()).orElseThrow().getPrincipal()).isFalse();
    }

    @Test
    void desmarcarPrincipalDiretamenteRetornaErro() throws Exception {
        Endereco principal = criarEndereco(cliente, true);

        mockMvc.perform(put("/endereco/" + principal.getId())
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("01310100", "SP", false)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void desmarcarPrincipalRetornaMensagemEMantemPrincipal() throws Exception {
        Endereco principal = criarEndereco(cliente, true);
        criarEndereco(cliente, false);

        mockMvc.perform(put("/endereco/" + principal.getId())
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("01310100", "SP", false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("marque outro endereço como principal"));

        recarregarContexto();
        assertThat(enderecoRepository.findById(principal.getId()).orElseThrow().getPrincipal()).isTrue();
    }

    @Test
    void excluirPrincipalPromoveOutroEndereco() throws Exception {
        Endereco principal = criarEndereco(cliente, true);
        Endereco secundario = criarEndereco(cliente, false);

        mockMvc.perform(delete("/endereco/" + principal.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isNoContent());

        recarregarContexto();
        assertThat(enderecoRepository.existsById(principal.getId())).isFalse();
        assertThat(enderecoRepository.findById(secundario.getId()).orElseThrow().getPrincipal()).isTrue();
    }

    @Test
    void administradorListaEnderecosDeUmCliente() throws Exception {
        criarEndereco(cliente, true);
        criarEndereco(outroCliente, true);

        mockMvc.perform(get("/cliente/" + cliente.getId() + "/enderecos").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].clienteId").value(cliente.getId().toString()));

        mockMvc.perform(get("/cliente/" + UUID.randomUUID() + "/enderecos").header("Authorization", bearer(administrador)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioComumNaoListaEnderecosPorCliente() throws Exception {
        mockMvc.perform(get("/cliente/" + outroCliente.getId() + "/enderecos").header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());
    }

    @Test
    void excluirClienteRemoveSeusEnderecos() throws Exception {
        Endereco endereco = criarEndereco(cliente, true);
        criarEndereco(cliente, false);

        mockMvc.perform(delete("/cliente/me").header("Authorization", bearer(usuario)))
                .andExpect(status().isNoContent());

        recarregarContexto();
        assertThat(clienteRepository.existsById(cliente.getId())).isFalse();
        assertThat(enderecoRepository.existsById(endereco.getId())).isFalse();
    }

    @Test
    void administradorExcluiClienteComEnderecos() throws Exception {
        criarEndereco(outroCliente, true);

        mockMvc.perform(delete("/cliente/" + outroCliente.getId()).header("Authorization", bearer(administrador)))
                .andExpect(status().isNoContent());

        recarregarContexto();
        assertThat(clienteRepository.existsById(outroCliente.getId())).isFalse();
        assertThat(enderecoRepository.existsByClienteId(outroCliente.getId())).isFalse();
    }

    private ResultActions cadastrar(Usuario autor, String body) throws Exception {
        return mockMvc.perform(post("/endereco")
                .header("Authorization", bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String json(String cep, String estado, Boolean principal) {
        return """
                {"cep":"%s","logradouro":"Av. Paulista","numero":"100","complemento":"Apto 1","bairro":"Bela Vista","cidade":"São Paulo","estado":"%s","principal":%s}
                """.formatted(cep, estado, principal).trim();
    }

    private String bearer(Usuario autor) {
        return "Bearer " + jwtService.gerarToken(autor);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
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
        novo.setDataNascimento(java.time.LocalDate.of(1990, 5, 10));
        novo.setTelefone("11987654321");
        return clienteRepository.saveAndFlush(novo);
    }

    private Endereco criarEndereco(Cliente dono, boolean principal) {
        Endereco endereco = new Endereco();
        endereco.setCliente(dono);
        endereco.setCep("01310100");
        endereco.setLogradouro("Av. Paulista");
        endereco.setNumero("100");
        endereco.setBairro("Bela Vista");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setPrincipal(principal);
        return enderecoRepository.saveAndFlush(endereco);
    }
}
