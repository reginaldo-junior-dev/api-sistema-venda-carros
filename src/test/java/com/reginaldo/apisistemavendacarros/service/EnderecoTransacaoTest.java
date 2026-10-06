package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Endereco;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.enums.PerfilUsuario;
import com.reginaldo.apisistemavendacarros.enums.ProvedorAutenticacao;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.EnderecoRepository;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Sem @Transactional: cada requisição faz commit ou rollback real
@SpringBootTest
@AutoConfigureMockMvc
class EnderecoTransacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @MockitoSpyBean
    private EnderecoRepository enderecoRepository;

    @Autowired
    private JwtService jwtService;

    private Usuario usuario;
    private Cliente cliente;
    private Endereco principal;
    private Endereco secundario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setNomeCompleto("Teste Transacao");
        usuario.setEmail(UUID.randomUUID() + "@teste.com");
        usuario.setPerfil(PerfilUsuario.USUARIO);
        usuario.setProvedor(ProvedorAutenticacao.LOCAL);
        usuario = usuarioRepository.save(usuario);

        cliente = new Cliente();
        cliente.setUsuario(usuario);
        cliente.setCpf(String.valueOf(10_000_000_000L + (long) (Math.random() * 89_999_999_999L)));
        cliente.setDataNascimento(LocalDate.of(1990, 5, 10));
        cliente.setTelefone("11987654321");
        cliente = clienteRepository.save(cliente);

        principal = enderecoRepository.save(novoEndereco(true));
        secundario = enderecoRepository.save(novoEndereco(false));
    }

    @AfterEach
    void limpar() {
        enderecoRepository.deleteAll(enderecoRepository.findAllByClienteId(cliente.getId()));
        clienteRepository.deleteById(cliente.getId());
        usuarioRepository.deleteById(usuario.getId());
    }

    @Test
    void trocaDePrincipalComCommitRealNaoViolaIndiceUnico() throws Exception {
        mockMvc.perform(put("/endereco/" + secundario.getId())
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(true)))
                .andExpect(status().isOk());

        assertThat(principaisDoCliente()).containsExactly(secundario.getId());
    }

    @Test
    void falhaAposDesmarcarPrincipalNaAtualizacaoFazRollback() {
        doThrow(new IllegalStateException("falha simulada")).when(enderecoRepository).save(any(Endereco.class));

        assertThatThrownBy(() -> mockMvc.perform(put("/endereco/" + secundario.getId())
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(true))))
                .hasRootCauseMessage("falha simulada");

        // O UPDATE que desmarcou o principal foi desfeito junto com a falha
        assertThat(principaisDoCliente()).containsExactly(principal.getId());
    }

    @Test
    void falhaAposDesmarcarPrincipalNoCadastroFazRollback() {
        doThrow(new IllegalStateException("falha simulada")).when(enderecoRepository).save(any(Endereco.class));

        assertThatThrownBy(() -> mockMvc.perform(post("/endereco")
                .header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(true))))
                .hasRootCauseMessage("falha simulada");

        assertThat(principaisDoCliente()).containsExactly(principal.getId());
        assertThat(enderecoRepository.findAllByClienteId(cliente.getId())).hasSize(2);
    }

    private List<UUID> principaisDoCliente() {
        return enderecoRepository.findAllByClienteId(cliente.getId()).stream()
                .filter(Endereco::getPrincipal)
                .map(Endereco::getId)
                .toList();
    }

    private Endereco novoEndereco(boolean ehPrincipal) {
        Endereco endereco = new Endereco();
        endereco.setCliente(cliente);
        endereco.setCep("01310100");
        endereco.setLogradouro("Av. Paulista");
        endereco.setNumero("100");
        endereco.setBairro("Bela Vista");
        endereco.setCidade("São Paulo");
        endereco.setEstado("SP");
        endereco.setPrincipal(ehPrincipal);
        return endereco;
    }

    private String json(boolean ehPrincipal) {
        return """
                {"cep":"20040020","logradouro":"Rua da Assembleia","numero":"10","bairro":"Centro","cidade":"Rio de Janeiro","estado":"RJ","principal":%s}
                """.formatted(ehPrincipal).trim();
    }

    private String bearer() {
        return "Bearer " + jwtService.gerarToken(usuario);
    }
}
