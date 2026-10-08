package com.reginaldo.apisistemavendacarros.dto.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

// Atualização dos próprios dados de cliente. Sem CPF: ele é definido no cadastro e não muda
public record ClienteAtualizacaoRequest(
        @NotNull(message = "Data de nascimento é obrigatória")
        @PastOrPresent(message = "Data de nascimento não pode ser uma data futura")
        LocalDate dataNascimento,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos numéricos")
        String telefone
) {
}
