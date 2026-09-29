package com.reginaldo.apisistemavendacarros.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ClienteRequest(
        @NotBlank(message = "CPF é obrigatório")
        @Pattern(regexp = "^(?!(\\d)\\1{10}$)\\d{11}$",
                message = "CPF deve conter exatamente 11 dígitos numéricos e não pode ser uma sequência repetida")
        String cpf,

        @NotNull(message = "Data de nascimento é obrigatória")
        @PastOrPresent(message = "Data de nascimento não pode ser uma data futura")
        LocalDate dataNascimento,

        @NotBlank(message = "Telefone é obrigatório")
        @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos numéricos")
        String telefone
) {
}
