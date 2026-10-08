package com.reginaldo.apisistemavendacarros.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

// Atualização dos próprios dados de cliente. CPF e data de nascimento são definidos no cadastro e não mudam
public record ClienteAtualizacaoRequest(
        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos numéricos")
        String telefone
) {
}
