package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static com.reginaldo.apisistemavendacarros.CsrfReal.tokenCsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private JwtService jwtService;

    private Usuario usuario;
    private Usuario outroUsuario;
    private Usuario administrador;

    @BeforeEach
    void setUp() {
        usuario = criarUsuario(PerfilUsuario.USUARIO);
        outroUsuario = criarUsuario(PerfilUsuario.USUARIO);
        administrador = criarUsuario(PerfilUsuario.ADMINISTRADOR);
    }

    @Test
    void usuarioAutenticadoCriaCliente() throws Exception {
        cadastrar(usuario, json("12345678901", "1990-05-10", "11987654321"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId().toString()));
    }

    @Test
    void usuarioIdEnviadoNoCorpoEhIgnorado() throws Exception {
        String body = """
                {"cpf":"12345678901","dataNascimento":"1990-05-10","telefone":"11987654321","usuarioId":"%s"}
                """.formatted(outroUsuario.getId());

        cadastrar(usuario, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId().toString()));

        assertThat(clienteRepository.existsByUsuarioId(outroUsuario.getId())).isFalse();
    }

    @Test
    void cadastroSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(post("/cliente").with(tokenCsrf(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("12345678901", "1990-05-10", "11987654321")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioNaoPodeCriarSegundoCliente() throws Exception {
        cadastrar(usuario, json("12345678901", "1990-05-10", "11987654321")).andExpect(status().isCreated());

        cadastrar(usuario, json("98765432100", "1990-05-10", "11987654321"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Usuário já possui um cliente cadastrado"));
    }

    @Test
    void cpfDuplicadoRetornaErro() throws Exception {
        cadastrar(usuario, json("12345678901", "1990-05-10", "11987654321")).andExpect(status().isCreated());

        cadastrar(outroUsuario, json("12345678901", "1985-01-01", "1133334444"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("CPF já cadastrado"));
    }

    @Test
    void cpfComLetrasRetornaErro() throws Exception {
        cadastrar(usuario, json("1234567890a", "1990-05-10", "11987654321"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.cpf").exists());
    }

    @Test
    void cpfComMenosDe11DigitosRetornaErro() throws Exception {
        cadastrar(usuario, json("1234567890", "1990-05-10", "11987654321"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.cpf").exists());
    }

    @Test
    void cpfComDigitosRepetidosRetornaErro() throws Exception {
        cadastrar(usuario, json("11111111111", "1990-05-10", "11987654321"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.cpf").exists());
    }

    @Test
    void cpfFicticioCom11DigitosFunciona() throws Exception {
        cadastrar(usuario, json("12312312312", "1990-05-10", "11987654321"))
                .andExpect(status().isCreated());
    }

    @Test
    void dataNascimentoFuturaRetornaErro() throws Exception {
        cadastrar(usuario, json("12345678901", LocalDate.now().plusDays(1).toString(), "11987654321"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.dataNascimento").exists());
    }

    @Test
    void telefoneInvalidoRetornaErro() throws Exception {
        cadastrar(usuario, json("12345678901", "1990-05-10", "119876"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.telefone").exists());

        cadastrar(usuario, json("12345678901", "1990-05-10", "(11)98765-4321"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.telefone").exists());
    }

    @Test
    void usuarioConsultaSomenteSeuCliente() throws Exception {
        criarCliente(usuario, "12345678901");
        criarCliente(outroUsuario, "98765432100");

        mockMvc.perform(get("/cliente/me").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId().toString()));
    }

    @Test
    void consultaSemClienteRetorna404() throws Exception {
        mockMvc.perform(get("/cliente/me").header("Authorization", bearer(usuario)))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioAtualizaSomenteSeuCliente() throws Exception {
        criarCliente(usuario, "12345678901");
        Cliente clienteOutro = criarCliente(outroUsuario, "98765432100");

        mockMvc.perform(put("/cliente/me")
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("12345678901", "1991-01-01", "1144445555")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefone").value("1144445555"))
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId().toString()));

        assertThat(clienteRepository.findById(clienteOutro.getId()).orElseThrow().getTelefone())
                .isEqualTo("11987654321");
    }

    @Test
    void cpfEnviadoNaAtualizacaoEhIgnorado() throws Exception {
        Cliente cliente = criarCliente(usuario, "12345678901");

        mockMvc.perform(put("/cliente/me")
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("52998224725", "1990-05-10", "1144445555")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.telefone").value("1144445555"));

        assertThat(clienteRepository.findById(cliente.getId()).orElseThrow().getCpf())
                .isEqualTo("12345678901");
    }

    @Test
    void usuarioExcluiSomenteSeuCliente() throws Exception {
        Cliente cliente = criarCliente(usuario, "12345678901");
        Cliente clienteOutro = criarCliente(outroUsuario, "98765432100");

        mockMvc.perform(delete("/cliente/me").header("Authorization", bearer(usuario)))
                .andExpect(status().isNoContent());

        assertThat(clienteRepository.existsById(cliente.getId())).isFalse();
        assertThat(clienteRepository.existsById(clienteOutro.getId())).isTrue();
    }

    @Test
    void usuarioComumNaoAcessaEndpointsAdministrativos() throws Exception {
        criarCliente(usuario, "12345678901");
        Cliente clienteOutro = criarCliente(outroUsuario, "98765432100");

        mockMvc.perform(get("/cliente/" + clienteOutro.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/cliente").header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/cliente/" + clienteOutro.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());

        assertThat(clienteRepository.existsById(clienteOutro.getId())).isTrue();
    }

    @Test
    void administradorRealizaOperacoesAdministrativas() throws Exception {
        Cliente cliente = criarCliente(usuario, "12345678901");

        mockMvc.perform(get("/cliente").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cliente/" + cliente.getId()).header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpf").value("12345678901"));

        mockMvc.perform(delete("/cliente/" + cliente.getId()).header("Authorization", bearer(administrador)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cliente/" + UUID.randomUUID()).header("Authorization", bearer(administrador)))
                .andExpect(status().isNotFound());
    }

    private ResultActions cadastrar(Usuario autor, String body) throws Exception {
        return mockMvc.perform(post("/cliente")
                .header("Authorization", bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String json(String cpf, String dataNascimento, String telefone) {
        return """
                {"cpf":"%s","dataNascimento":"%s","telefone":"%s"}
                """.formatted(cpf, dataNascimento, telefone);
    }

    private String bearer(Usuario autor) {
        return "Bearer " + jwtService.gerarToken(autor);
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
        Cliente cliente = new Cliente();
        cliente.setUsuario(dono);
        cliente.setCpf(cpf);
        cliente.setDataNascimento(LocalDate.of(1990, 5, 10));
        cliente.setTelefone("11987654321");
        return clienteRepository.saveAndFlush(cliente);
    }
}
