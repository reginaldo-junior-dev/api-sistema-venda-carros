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

import static com.reginaldo.apisistemavendacarros.CsrfReal.tokenCsrf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InteresseCarroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private InteresseCarroRepository interesseCarroRepository;

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
        carro = criarCarro(StatusCarro.DISPONIVEL);
    }

    @Test
    void usuarioAutenticadoRegistraInteresse() throws Exception {
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        cadastrar(usuario, carro.getId(), json("João da Silva", "joao@email.com", "21999999999", "Tenho interesse."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.carroId").value(carro.getId().toString()))
                .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.nome").value("João da Silva"))
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                .andExpect(jsonPath("$.telefone").value("21999999999"))
                .andExpect(jsonPath("$.mensagem").value("Tenho interesse."))
                .andExpect(jsonPath("$.status").value("NOVO"))
                .andExpect(jsonPath("$.dataInteresse").exists());

        recarregarContexto();
        InteresseCarro salvo = interesseCarroRepository.findByClienteId(cliente.getId()).getFirst();
        assertThat(salvo.getStatus()).isEqualTo(StatusInteresse.NOVO);
        assertThat(salvo.getDataInteresse()).isAfter(antes).isBefore(LocalDateTime.now().plusSeconds(1));
        assertThat(salvo.getNome()).isEqualTo("João da Silva");
        assertThat(salvo.getEmail()).isEqualTo("joao@email.com");
        assertThat(salvo.getTelefone()).isEqualTo("21999999999");
    }

    @Test
    void camposDeBackendEnviadosNoCorpoSaoIgnorados() throws Exception {
        String body = """
                {"nome":"João","email":"joao@email.com","telefone":"21999999999","mensagem":"Oi",
                 "clienteId":"%s","status":"CONVERTIDO","dataInteresse":"2000-01-01T00:00:00"}
                """.formatted(outroCliente.getId());

        cadastrar(usuario, carro.getId(), body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clienteId").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.status").value("NOVO"));

        assertThat(interesseCarroRepository.findByClienteId(outroCliente.getId())).isEmpty();
        assertThat(interesseCarroRepository.findByClienteId(cliente.getId()).getFirst().getDataInteresse().getYear())
                .isNotEqualTo(2000);
    }

    @Test
    void dadosDeContatoSaoSnapshotEIndependemDoCliente() throws Exception {
        cadastrar(usuario, carro.getId(), json("João", "joao@email.com", "21999999999", "Oi"))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/cliente/me")
                        .header("Authorization", bearer(usuario))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telefone":"11888887777"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/cliente/me/interesses").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].telefone").value("21999999999"))
                .andExpect(jsonPath("$.content[0].nome").value("João"));
    }

    @Test
    void carroInexistenteRetorna404() throws Exception {
        cadastrar(usuario, UUID.randomUUID(), json("João", "joao@email.com", "21999999999", "Oi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Carro não encontrado"));
    }

    @Test
    void carroVendidoRetorna400() throws Exception {
        Carro vendido = criarCarro(StatusCarro.VENDIDO);

        cadastrar(usuario, vendido.getId(), json("João", "joao@email.com", "21999999999", "Oi"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Não é possível registrar interesse em um carro vendido"));

        assertThat(interesseCarroRepository.findByClienteId(cliente.getId())).isEmpty();
    }

    @Test
    void usuarioSemClienteRetorna404() throws Exception {
        Usuario semCliente = criarUsuario(PerfilUsuario.USUARIO);

        cadastrar(semCliente, carro.getId(), json("João", "joao@email.com", "21999999999", "Oi"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado"));
    }

    @Test
    void cadastroSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(post("/carro/" + carro.getId() + "/interesse").with(tokenCsrf(mockMvc))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("João", "joao@email.com", "21999999999", "Oi")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void camposObrigatoriosVaziosRetornam400() throws Exception {
        cadastrar(usuario, carro.getId(), json("", "", "", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.nome").exists())
                .andExpect(jsonPath("$.mensagens.email").exists())
                .andExpect(jsonPath("$.mensagens.telefone").exists())
                .andExpect(jsonPath("$.mensagens.mensagem").exists());
    }

    @Test
    void emailInvalidoRetorna400() throws Exception {
        cadastrar(usuario, carro.getId(), json("João", "email-invalido", "21999999999", "Oi"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.email").value("Email inválido"));
    }

    @Test
    void telefoneInvalidoRetorna400() throws Exception {
        cadastrar(usuario, carro.getId(), json("João", "joao@email.com", "(21)99999-9999", "Oi"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.telefone").exists());

        cadastrar(usuario, carro.getId(), json("João", "joao@email.com", "219999", "Oi"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.telefone").exists());
    }

    @Test
    void mensagemMaiorQueColunaRetorna400() throws Exception {
        cadastrar(usuario, carro.getId(), json("João", "joao@email.com", "21999999999", "a".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.mensagem").exists());
    }

    @Test
    void clienteListaSomenteSeusInteresses() throws Exception {
        criarInteresse(cliente, "Meu interesse");
        criarInteresse(cliente, "Outro meu");
        criarInteresse(outroCliente, "De outra pessoa");

        mockMvc.perform(get("/cliente/me/interesses").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].clienteId").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.content[1].clienteId").value(cliente.getId().toString()));
    }

    @Test
    void listaVaziaRetornaNormalmente() throws Exception {
        mockMvc.perform(get("/cliente/me/interesses").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void listarSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/cliente/me/interesses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorListaInteresses() throws Exception {
        InteresseCarro meu = criarInteresse(cliente, "A");
        InteresseCarro deOutro = criarInteresse(outroCliente, "B");

        mockMvc.perform(get("/interesse").header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(meu.getId())).exists())
                .andExpect(jsonPath("$.content[?(@.id == '%s')]".formatted(deOutro.getId())).exists());
    }

    @Test
    void administradorBuscaInteressePorId() throws Exception {
        InteresseCarro interesse = criarInteresse(cliente, "Quero ver o carro");

        mockMvc.perform(get("/interesse/" + interesse.getId()).header("Authorization", bearer(administrador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(interesse.getId().toString()))
                .andExpect(jsonPath("$.mensagem").value("Quero ver o carro"));

        mockMvc.perform(get("/interesse/" + UUID.randomUUID()).header("Authorization", bearer(administrador)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Interesse não encontrado"));
    }

    @ParameterizedTest
    @EnumSource(StatusInteresse.class)
    void administradorAlteraParaQualquerStatus(StatusInteresse novoStatus) throws Exception {
        InteresseCarro interesse = criarInteresse(cliente, "Oi");

        atualizarStatus(administrador, interesse.getId(), "{\"status\":\"%s\"}".formatted(novoStatus))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(novoStatus.name()));

        recarregarContexto();
        assertThat(interesseCarroRepository.findById(interesse.getId()).orElseThrow().getStatus()).isEqualTo(novoStatus);
    }

    @Test
    void alteracaoDeStatusNaoAlteraDemaisDados() throws Exception {
        InteresseCarro interesse = criarInteresse(cliente, "Mensagem original");
        recarregarContexto();
        InteresseCarro original = interesseCarroRepository.findById(interesse.getId()).orElseThrow();

        String body = """
                {"status":"EM_CONTATO","nome":"Outro","email":"outro@email.com","telefone":"11000000000",
                 "mensagem":"Alterada","clienteId":"%s","carroId":"%s","dataInteresse":"2000-01-01T00:00:00"}
                """.formatted(outroCliente.getId(), UUID.randomUUID());

        atualizarStatus(administrador, interesse.getId(), body)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_CONTATO"));

        recarregarContexto();
        InteresseCarro depois = interesseCarroRepository.findById(interesse.getId()).orElseThrow();
        assertThat(depois.getStatus()).isEqualTo(StatusInteresse.EM_CONTATO);
        assertThat(depois.getNome()).isEqualTo(original.getNome());
        assertThat(depois.getEmail()).isEqualTo(original.getEmail());
        assertThat(depois.getTelefone()).isEqualTo(original.getTelefone());
        assertThat(depois.getMensagem()).isEqualTo(original.getMensagem());
        assertThat(depois.getDataInteresse()).isEqualTo(original.getDataInteresse());
        assertThat(depois.getCliente().getId()).isEqualTo(cliente.getId());
        assertThat(depois.getCarro().getId()).isEqualTo(carro.getId());
    }

    @Test
    void statusNuloOuInvalidoRetorna400() throws Exception {
        InteresseCarro interesse = criarInteresse(cliente, "Oi");

        atualizarStatus(administrador, interesse.getId(), "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagens.status").exists());

        atualizarStatus(administrador, interesse.getId(), "{\"status\":\"INEXISTENTE\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void usuarioComumRecebe403NosEndpointsAdministrativos() throws Exception {
        InteresseCarro deOutro = criarInteresse(outroCliente, "Oi");

        mockMvc.perform(get("/interesse").header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/interesse/" + deOutro.getId()).header("Authorization", bearer(usuario)))
                .andExpect(status().isForbidden());

        atualizarStatus(usuario, deOutro.getId(), "{\"status\":\"CANCELADO\"}")
                .andExpect(status().isForbidden());

        recarregarContexto();
        assertThat(interesseCarroRepository.findById(deOutro.getId()).orElseThrow().getStatus())
                .isEqualTo(StatusInteresse.NOVO);
    }

    @Test
    void endpointsAdministrativosSemAutenticacaoRetornam401() throws Exception {
        mockMvc.perform(get("/interesse"))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions cadastrar(Usuario autor, UUID carroId, String body) throws Exception {
        return mockMvc.perform(post("/carro/" + carroId + "/interesse")
                .header("Authorization", bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions atualizarStatus(Usuario autor, UUID id, String body) throws Exception {
        return mockMvc.perform(put("/interesse/" + id + "/status")
                .header("Authorization", bearer(autor))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String json(String nome, String email, String telefone, String mensagem) {
        return """
                {"nome":"%s","email":"%s","telefone":"%s","mensagem":"%s"}
                """.formatted(nome, email, telefone, mensagem).trim();
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
        novo.setDataNascimento(LocalDate.of(1990, 5, 10));
        novo.setTelefone("11987654321");
        return clienteRepository.saveAndFlush(novo);
    }

    private Carro criarCarro(StatusCarro status) {
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
        novo.setPreco(new BigDecimal("50000.00"));
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

    private InteresseCarro criarInteresse(Cliente dono, String mensagem) {
        InteresseCarro interesse = new InteresseCarro();
        interesse.setCliente(dono);
        interesse.setCarro(carro);
        interesse.setNome("Contato Teste");
        interesse.setEmail("contato@email.com");
        interesse.setTelefone("21999999999");
        interesse.setMensagem(mensagem);
        interesse.setStatus(StatusInteresse.NOVO);
        return interesseCarroRepository.saveAndFlush(interesse);
    }
}
