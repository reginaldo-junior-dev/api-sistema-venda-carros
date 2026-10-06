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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FavoritoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private FavoritoRepository favoritoRepository;

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
    private Cliente cliente;
    private Cliente outroCliente;
    private Carro carro;
    private Carro outroCarro;

    @BeforeEach
    void setUp() {
        usuario = criarUsuario();
        outroUsuario = criarUsuario();
        cliente = criarCliente(usuario, "12345678901");
        outroCliente = criarCliente(outroUsuario, "98765432100");
        carro = criarCarro(StatusCarro.DISPONIVEL);
        outroCarro = criarCarro(StatusCarro.DISPONIVEL);
    }

    @Test
    void usuarioAutenticadoFavoritaCarro() throws Exception {
        mockMvc.perform(post("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.carroId").value(carro.getId().toString()))
                .andExpect(jsonPath("$.dataFavorito").exists())
                .andExpect(jsonPath("$.clienteId").doesNotExist());

        assertThat(favoritoRepository.existsByClienteIdAndCarroId(cliente.getId(), carro.getId())).isTrue();
        assertThat(favoritoRepository.existsByClienteIdAndCarroId(outroCliente.getId(), carro.getId())).isFalse();
    }

    @Test
    void favoritarCarroInexistenteRetorna404() throws Exception {
        mockMvc.perform(post("/carro/" + UUID.randomUUID() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Carro não encontrado"));
    }

    @Test
    void usuarioSemClienteRetorna404() throws Exception {
        Usuario semCliente = criarUsuario();

        mockMvc.perform(post("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(semCliente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado"));
    }

    @Test
    void favoritarMesmoCarroDuasVezesRetorna409() throws Exception {
        mockMvc.perform(post("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Carro já está nos favoritos"));

        assertThat(favoritoRepository.findByClienteId(cliente.getId())).hasSize(1);
    }

    @Test
    void clientesDiferentesPodemFavoritarMesmoCarro() throws Exception {
        mockMvc.perform(post("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(outroUsuario)))
                .andExpect(status().isCreated());
    }

    @Test
    void favoritarCarroVendidoRetorna400() throws Exception {
        Carro vendido = criarCarro(StatusCarro.VENDIDO);

        mockMvc.perform(post("/carro/" + vendido.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Não é possível favoritar um carro vendido"));

        assertThat(favoritoRepository.existsByClienteIdAndCarroId(cliente.getId(), vendido.getId())).isFalse();
    }

    @Test
    void favoritarSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(post("/carro/" + carro.getId() + "/favorito"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioRemoveSeuFavorito() throws Exception {
        criarFavorito(cliente, carro);

        mockMvc.perform(delete("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isNoContent());

        recarregarContexto();
        assertThat(favoritoRepository.existsByClienteIdAndCarroId(cliente.getId(), carro.getId())).isFalse();
    }

    @Test
    void removerFavoritoInexistenteRetorna404() throws Exception {
        mockMvc.perform(delete("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Favorito não encontrado"));
    }

    @Test
    void usuarioNaoRemoveFavoritoDeOutroCliente() throws Exception {
        criarFavorito(outroCliente, carro);

        mockMvc.perform(delete("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isNotFound());

        recarregarContexto();
        assertThat(favoritoRepository.existsByClienteIdAndCarroId(outroCliente.getId(), carro.getId())).isTrue();
    }

    @Test
    void removerSomenteFavoritoDoProprioClienteQuandoAmbosFavoritaram() throws Exception {
        criarFavorito(cliente, carro);
        criarFavorito(outroCliente, carro);

        mockMvc.perform(delete("/carro/" + carro.getId() + "/favorito").header("Authorization", bearer(usuario)))
                .andExpect(status().isNoContent());

        recarregarContexto();
        assertThat(favoritoRepository.existsByClienteIdAndCarroId(cliente.getId(), carro.getId())).isFalse();
        assertThat(favoritoRepository.existsByClienteIdAndCarroId(outroCliente.getId(), carro.getId())).isTrue();
    }

    @Test
    void removerSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(delete("/carro/" + carro.getId() + "/favorito"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioListaSeusFavoritos() throws Exception {
        criarFavorito(cliente, carro);
        criarFavorito(cliente, outroCarro);

        mockMvc.perform(get("/cliente/me/favoritos").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void listaVaziaRetornaNormalmente() throws Exception {
        mockMvc.perform(get("/cliente/me/favoritos").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void listaNaoRetornaFavoritosDeOutroCliente() throws Exception {
        criarFavorito(cliente, carro);
        criarFavorito(outroCliente, outroCarro);

        mockMvc.perform(get("/cliente/me/favoritos").header("Authorization", bearer(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].carroId").value(carro.getId().toString()));
    }

    @Test
    void listarSemAutenticacaoRetorna401() throws Exception {
        mockMvc.perform(get("/cliente/me/favoritos"))
                .andExpect(status().isUnauthorized());
    }

    private String bearer(Usuario autor) {
        return "Bearer " + jwtService.gerarToken(autor);
    }

    private void recarregarContexto() {
        entityManager.flush();
        entityManager.clear();
    }

    private Usuario criarUsuario() {
        Usuario novo = new Usuario();
        novo.setNomeCompleto("Teste Favorito");
        novo.setEmail(UUID.randomUUID() + "@teste.com");
        novo.setPerfil(PerfilUsuario.USUARIO);
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

    private void criarFavorito(Cliente dono, Carro alvo) {
        Favorito favorito = new Favorito();
        favorito.setCliente(dono);
        favorito.setCarro(alvo);
        favoritoRepository.saveAndFlush(favorito);
    }
}
