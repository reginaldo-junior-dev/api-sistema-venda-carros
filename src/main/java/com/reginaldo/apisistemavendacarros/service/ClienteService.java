package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.ClienteRequest;
import com.reginaldo.apisistemavendacarros.dto.ClienteResponse;
import com.reginaldo.apisistemavendacarros.entity.Cliente;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.mapper.ClienteMapper;
import com.reginaldo.apisistemavendacarros.repository.ClienteRepository;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteMapper mapper;

    public ClienteResponse cadastro (ClienteRequest request) {
        Usuario usuario = usuarioRepository.findById(request.usuarioId()).orElseThrow(() ->
                new RuntimeException("Usuário não encontrado"));

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
                new RuntimeException("Cliente não encontrado"));

        return mapper.toResponse(cliente);
    }

    public ClienteResponse atualizar (UUID id, ClienteRequest request) {
        Cliente cliente = clienteRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Cliente não encontrado"));

        Usuario usuario = usuarioRepository.findById(request.usuarioId()).orElseThrow(() ->
                new RuntimeException("Usuário não encontrado"));

        mapper.atualizar(request, cliente);
        cliente.setUsuario(usuario);

        clienteRepository.save(cliente);

        return mapper.toResponse(cliente);
    }

    public void excluir (UUID id) {
        clienteRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Cliente não encontrado"));

        clienteRepository.deleteById(id);
    }
}
