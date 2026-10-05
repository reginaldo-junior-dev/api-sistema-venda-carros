package com.reginaldo.apisistemavendacarros.event;

// Publicado no cadastro; os ouvintes só reagem depois do commit
public record UsuarioCadastradoEvent(String email, String nome) {
}
