package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.ClienteResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.exception.ConflitoException;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.ClienteMapper;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.CompraRepository;
import com.reginaldo.apisistemavendacarros.repository.EnderecoRepository;
import com.reginaldo.apisistemavendacarros.repository.FavoritoRepository;
import com.reginaldo.apisistemavendacarros.repository.InteresseCarroRepository;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final FavoritoRepository favoritoRepository;
    private final InteresseCarroRepository interesseCarroRepository;
    private final CompraRepository compraRepository;
    private final ClienteMapper mapper;

    @Transactional
    public ClienteResponse cadastro (UUID usuarioId, ClienteRequest request) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Usuário não encontrado"));

        if (clienteRepository.existsByUsuarioId(usuarioId)) {
            throw new ConflitoException("Usuário já possui um cliente cadastrado");
        }

        if (clienteRepository.existsByCpf(request.cpf())) {
            throw new ConflitoException("CPF já cadastrado");
        }

        Cliente cliente = mapper.toEntity(request);
        cliente.setUsuario(usuario);

        clienteRepository.save(cliente);

        return mapper.toResponse(cliente);
    }

    public List<ClienteResponse> listar () {
        List<Cliente> clientes = clienteRepository.findAll();
        return clientes.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public ClienteResponse buscarPorId (UUID id) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        return mapper.toResponse(cliente);
    }

    public ClienteResponse buscarPorUsuario (UUID usuarioId) {
        return mapper.toResponse(buscarClienteDoUsuario(usuarioId));
    }

    @Transactional
    public ClienteResponse atualizarPorUsuario (UUID usuarioId, ClienteRequest request) {
        Cliente cliente = buscarClienteDoUsuario(usuarioId);

        if (clienteRepository.existsByCpfAndIdNot(request.cpf(), cliente.getId())) {
            throw new ConflitoException("CPF já cadastrado");
        }

        mapper.atualizar(request, cliente);

        clienteRepository.save(cliente);

        return mapper.toResponse(cliente);
    }

    @Transactional
    public void excluirPorUsuario (UUID usuarioId) {
        excluirComDependentes(buscarClienteDoUsuario(usuarioId));
    }

    @Transactional
    public void excluir (UUID id) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));

        excluirComDependentes(cliente);
    }

    // Usado na exclusão do Usuario: remove o Cliente dele, se existir, pelas mesmas regras
    @Transactional
    public void excluirSeExistir (UUID usuarioId) {
        clienteRepository.findByUsuarioId(usuarioId).ifPresent(this::excluirComDependentes);
    }

    // Compras (e seus pagamentos/parcelas) são histórico financeiro e bloqueiam a exclusão.
    // Favoritos, interesses e endereços não são histórico e são apagados junto.
    private void excluirComDependentes (Cliente cliente) {
        if (compraRepository.existsByClienteId(cliente.getId())) {
            throw new ValorInvalidoException("Cliente possui compras registradas e não pode ser excluído");
        }

        favoritoRepository.deleteByClienteId(cliente.getId());
        interesseCarroRepository.deleteByClienteId(cliente.getId());
        enderecoRepository.deleteByClienteId(cliente.getId());
        clienteRepository.delete(cliente);
    }

    private Cliente buscarClienteDoUsuario (UUID usuarioId) {
        return clienteRepository.findByUsuarioId(usuarioId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Cliente não encontrado"));
    }
}
