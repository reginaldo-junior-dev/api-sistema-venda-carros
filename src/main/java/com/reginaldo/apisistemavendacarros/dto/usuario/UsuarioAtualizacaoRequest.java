package com.reginaldo.apisistemavendacarros.dto.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Atualização da própria conta. Trocar e-mail ou senha exige a senha atual:
// quem pegar um token emprestado não consegue tomar a conta
public record UsuarioAtualizacaoRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nomeCompleto,

        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        @Size(max = 150, message = "Email deve ter no máximo 150 caracteres")
        String email,

        @Size(max = 72, message = "Senha deve ter no máximo 72 caracteres")
        String senhaAtual,

        // Vazia mantém a senha atual
        @Size(min = 8, max = 72, message = "Senha deve ter entre 8 e 72 caracteres")
        String novaSenha
) {
}
