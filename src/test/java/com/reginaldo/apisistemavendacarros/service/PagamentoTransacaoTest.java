package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional: cada requisição faz commit ou rollback real
@SpringBootTest
@AutoConfigureMockMvc
class PagamentoTransacaoTest {

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

    private Usuario administrador;
    private Usuario usuario;
    private Cliente cliente;
    private Marca marca;
    private Modelo modelo;
    private Cor cor;
    private Categoria categoria;
    private Carro carro;
    private Compra compra;
    private Pagamento pagamento;

    @BeforeEach
    void setUp() {
        administrador = usuarioRepository.save(novoUsuario(PerfilUsuario.ADMINISTRADOR));
        usuario = usuarioRepository.save(novoUsuario(PerfilUsuario.USUARIO));

        cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setCpf(String.valueOf(10_000_000_000L + (long) (Math.random() * 89_999_999_999L)));
        cliente.setDataNascimento(LocalDate.of(1990, 5, 10));
        cliente.setTelefone("11987654321");
        cliente = clienteRepository.save(cliente);

        String sufixo = UUID.randomUUID().toString().substring(0, 8);
        marca = new Marca();
        marca.setNome("Marca " + sufixo);
        marca = marcaRepository.save(marca);
        modelo = new Modelo();
        modelo.setNome("Modelo " + sufixo);
        modelo.setMarca(marca);
        modelo = modeloRepository.save(modelo);
        cor = new Cor();
        cor.setNome("Cor " + sufixo);
        cor = corRepository.save(cor);
        categoria = new Categoria();
        categoria.setNome("Categoria " + sufixo);
        categoria = categoriaRepository.save(categoria);

        carro = new Carro();
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
        carro = carroRepository.save(carro);

        compra = new Compra();
        compra.setCliente(cliente);
        compra.setCarro(carro);
        compra.setValorTotal(carro.getPreco());
        compra.setStatus(StatusCompra.PENDENTE);
        compra = compraRepository.save(compra);

        pagamento = new Pagamento();
        pagamento.setCompra(compra);
        pagamento.setMetodo(MetodoPagamento.PIX);
        pagamento.setValor(compra.getValorTotal());
        pagamento.setStatus(StatusPagamento.PENDENTE);
        pagamento = pagamentoRepository.save(pagamento);
    }

    @AfterEach
    void limpar() {
        pagamentoRepository.deleteAll(pagamentoRepository.findByCompraClienteId(cliente.getId()));
        compraRepository.deleteById(compra.getId());
        carroRepository.deleteById(carro.getId());
        categoriaRepository.deleteById(categoria.getId());
        corRepository.deleteById(cor.getId());
        modeloRepository.deleteById(modelo.getId());
        marcaRepository.deleteById(marca.getId());
        clienteRepository.deleteById(cliente.getId());
        usuarioRepository.deleteById(usuario.getId());
        usuarioRepository.deleteById(administrador.getId());
    }

    @Test
    void aprovacaoComCommitRealAtualizaPagamentoCompraECarro() throws Exception {
        mockMvc.perform(put("/pagamento/" + pagamento.getId() + "/aprovar").header("Authorization", bearer()))
                .andExpect(status().isOk());

        Pagamento aprovado = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
        assertThat(aprovado.getStatus()).isEqualTo(StatusPagamento.APROVADO);
        assertThat(aprovado.getDataPagamento()).isNotNull();
        assertThat(compraRepository.findById(compra.getId()).orElseThrow().getStatus()).isEqualTo(StatusCompra.APROVADA);
        assertThat(carroRepository.findById(carro.getId()).orElseThrow().getStatus()).isEqualTo(StatusCarro.VENDIDO);
    }

    @Test
    void falhaNaAprovacaoDaCompraDesfazAprovacaoDoPagamento() throws Exception {
        // Carro fora de RESERVADO faz a aprovação da compra falhar depois do pagamento já estar APROVADO
        carro.setStatus(StatusCarro.DISPONIVEL);
        carroRepository.save(carro);

        mockMvc.perform(put("/pagamento/" + pagamento.getId() + "/aprovar").header("Authorization", bearer()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("Carro da compra não está reservado"));

        Pagamento depois = pagamentoRepository.findById(pagamento.getId()).orElseThrow();
        assertThat(depois.getStatus()).isEqualTo(StatusPagamento.PENDENTE);
        assertThat(depois.getDataPagamento()).isNull();
        assertThat(compraRepository.findById(compra.getId()).orElseThrow().getStatus()).isEqualTo(StatusCompra.PENDENTE);
    }

    private Usuario novoUsuario(PerfilUsuario perfil) {
        Usuario novo = new Usuario();
        novo.setNomeCompleto("Teste Transacao " + perfil);
        novo.setEmail(UUID.randomUUID() + "@teste.com");
        novo.setPerfil(perfil);
        novo.setProvedor(ProvedorAutenticacao.LOCAL);
        return novo;
    }

    private String bearer() {
        return "Bearer " + jwtService.gerarToken(administrador);
    }
}
