package com.reginaldo.apisistemavendacarros;

import com.reginaldo.apisistemavendacarros.entity.*;
import com.reginaldo.apisistemavendacarros.enums.*;
import com.reginaldo.apisistemavendacarros.repository.*;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

// Cria os dados dos testes. Nos testes sem @Transactional, limpar() apaga tudo, inclusive o que a API criou
@TestComponent
@RequiredArgsConstructor
public class DadosTeste {

    public static final String SENHA = "senha123";

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final CarroRepository carroRepository;
    private final MarcaRepository marcaRepository;
    private final ModeloRepository modeloRepository;
    private final CorRepository corRepository;
    private final CategoriaRepository categoriaRepository;
    private final CompraRepository compraRepository;
    private final PagamentoRepository pagamentoRepository;
    private final FavoritoRepository favoritoRepository;
    private final InteresseCarroRepository interesseCarroRepository;
    private final EnderecoRepository enderecoRepository;
    private final ImagemCarroRepository imagemCarroRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    private final List<UUID> usuarios = new ArrayList<>();
    private final List<UUID> clientes = new ArrayList<>();
    private final List<UUID> carros = new ArrayList<>();
    private final List<UUID> modelos = new ArrayList<>();
    private final List<UUID> marcas = new ArrayList<>();
    private final List<UUID> cores = new ArrayList<>();
    private final List<UUID> categorias = new ArrayList<>();

    public Usuario usuario(PerfilUsuario perfil) {
        Usuario novo = new Usuario();
        novo.setNomeCompleto("Teste " + perfil);
        novo.setEmail(UUID.randomUUID() + "@teste.com");
        novo.setSenha(passwordEncoder.encode(SENHA));
        novo.setPerfil(perfil);
        novo.setProvedor(ProvedorAutenticacao.LOCAL);
        novo = usuarioRepository.saveAndFlush(novo);
        usuarios.add(novo.getId());
        return novo;
    }

    public Cliente cliente(Usuario dono) {
        Cliente novo = new Cliente();
        novo.setUsuario(dono);
        // CPF aleatório para não colidir entre testes com commit real
        novo.setCpf(String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L)));
        novo.setDataNascimento(LocalDate.of(1990, 5, 10));
        novo.setTelefone("11987654321");
        novo = clienteRepository.saveAndFlush(novo);
        clientes.add(novo.getId());
        return novo;
    }

    public Carro carro(StatusCarro status) {
        String sufixo = UUID.randomUUID().toString().substring(0, 8);

        Marca marca = new Marca();
        marca.setNome("Marca " + sufixo);
        marca = marcaRepository.save(marca);
        marcas.add(marca.getId());

        Modelo modelo = new Modelo();
        modelo.setNome("Modelo " + sufixo);
        modelo.setMarca(marca);
        modelo = modeloRepository.save(modelo);
        modelos.add(modelo.getId());

        Cor cor = new Cor();
        cor.setNome("Cor " + sufixo);
        cor = corRepository.save(cor);
        cores.add(cor.getId());

        Categoria categoria = new Categoria();
        categoria.setNome("Categoria " + sufixo);
        categoria = categoriaRepository.save(categoria);
        categorias.add(categoria.getId());

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
        novo = carroRepository.saveAndFlush(novo);
        carros.add(novo.getId());
        return novo;
    }

    public Compra compra(Cliente dono, Carro carro, StatusCompra status) {
        Compra nova = new Compra();
        nova.setCliente(dono);
        nova.setCarro(carro);
        nova.setValorTotal(carro.getPreco());
        nova.setStatus(status);
        return compraRepository.saveAndFlush(nova);
    }

    public Pagamento pagamento(Compra compra, StatusPagamento status) {
        Pagamento novo = new Pagamento();
        novo.setCompra(compra);
        novo.setMetodo(MetodoPagamento.PIX);
        novo.setValor(compra.getValorTotal());
        novo.setStatus(status);
        if (status == StatusPagamento.APROVADO) {
            novo.setDataPagamento(LocalDateTime.now());
        }
        return pagamentoRepository.saveAndFlush(novo);
    }

    public Favorito favorito(Cliente dono, Carro carro) {
        Favorito novo = new Favorito();
        novo.setCliente(dono);
        novo.setCarro(carro);
        return favoritoRepository.saveAndFlush(novo);
    }

    public InteresseCarro interesse(Cliente dono, Carro carro) {
        InteresseCarro novo = new InteresseCarro();
        novo.setCliente(dono);
        novo.setCarro(carro);
        novo.setNome("Contato");
        novo.setEmail("contato@email.com");
        novo.setTelefone("21999999999");
        novo.setMensagem("Tenho interesse");
        novo.setStatus(StatusInteresse.NOVO);
        return interesseCarroRepository.saveAndFlush(novo);
    }

    public Endereco endereco(Cliente dono) {
        Endereco novo = new Endereco();
        novo.setCliente(dono);
        novo.setCep("01310100");
        novo.setLogradouro("Av. Paulista");
        novo.setNumero("100");
        novo.setBairro("Bela Vista");
        novo.setCidade("São Paulo");
        novo.setEstado("SP");
        novo.setPrincipal(true);
        return enderecoRepository.saveAndFlush(novo);
    }

    public ImagemCarro imagem(Carro carro, String chaveArquivo) {
        ImagemCarro nova = new ImagemCarro();
        nova.setCarro(carro);
        nova.setChaveArquivo(chaveArquivo);
        nova.setOrdem(1);
        nova.setPrincipal(true);
        return imagemCarroRepository.saveAndFlush(nova);
    }

    public String bearer(Usuario usuario) {
        return "Bearer " + jwtService.gerarToken(usuario);
    }

    // Apaga na ordem das FKs, inclusive compras, pagamentos e parcelas criados pela API
    public void limpar() {
        transactionTemplate.executeWithoutResult(status -> {
            List<UUID> cli = semVazio(clientes);
            List<UUID> car = semVazio(carros);

            executar("DELETE FROM Parcela p WHERE p.pagamento.id IN (SELECT pg.id FROM Pagamento pg "
                    + "WHERE pg.compra.cliente.id IN :cli OR pg.compra.carro.id IN :car)", cli, car);
            executar("DELETE FROM Pagamento pg WHERE pg.compra.id IN (SELECT c.id FROM Compra c "
                    + "WHERE c.cliente.id IN :cli OR c.carro.id IN :car)", cli, car);
            executar("DELETE FROM Compra c WHERE c.cliente.id IN :cli OR c.carro.id IN :car", cli, car);
            executar("DELETE FROM Favorito f WHERE f.cliente.id IN :cli OR f.carro.id IN :car", cli, car);
            executar("DELETE FROM InteresseCarro i WHERE i.cliente.id IN :cli OR i.carro.id IN :car", cli, car);
            executar("DELETE FROM Endereco e WHERE e.cliente.id IN :cli", cli, car);
            executar("DELETE FROM ImagemCarro i WHERE i.carro.id IN :car", cli, car);

            apagarPorId("Carro", carros);
            apagarPorId("Modelo", modelos);
            apagarPorId("Marca", marcas);
            apagarPorId("Cor", cores);
            apagarPorId("Categoria", categorias);
            if (!usuarios.isEmpty()) {
                entityManager.createQuery("DELETE FROM Cliente c WHERE c.usuario.id IN :ids")
                        .setParameter("ids", usuarios).executeUpdate();
            }
            apagarPorId("Cliente", clientes);
            apagarPorId("Usuario", usuarios);
        });

        List.of(usuarios, clientes, carros, modelos, marcas, cores, categorias).forEach(List::clear);
    }

    private void executar(String jpql, List<UUID> cli, List<UUID> car) {
        var query = entityManager.createQuery(jpql);
        if (jpql.contains(":cli")) query.setParameter("cli", cli);
        if (jpql.contains(":car")) query.setParameter("car", car);
        query.executeUpdate();
    }

    private void apagarPorId(String entidade, List<UUID> ids) {
        if (!ids.isEmpty()) {
            entityManager.createQuery("DELETE FROM " + entidade + " x WHERE x.id IN :ids")
                    .setParameter("ids", ids).executeUpdate();
        }
    }

    // IN () vazio é inválido em SQL; um UUID aleatório nunca casa com nada
    private List<UUID> semVazio(List<UUID> ids) {
        return ids.isEmpty() ? List.of(UUID.randomUUID()) : List.copyOf(ids);
    }
}
