package com.reginaldo.apisistemavendacarros.dto.endereco;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EnderecoRequest(
        @NotBlank(message = "CEP é obrigatório")
        @Pattern(regexp = "^\\d{8}$", message = "CEP deve conter exatamente 8 dígitos numéricos")
        String cep,

        @NotBlank(message = "Logradouro é obrigatório")
        @Size(max = 100, message = "Logradouro deve ter no máximo 100 caracteres")
        String logradouro,

        @NotBlank(message = "Número é obrigatório")
        @Size(max = 10, message = "Número deve ter no máximo 10 caracteres")
        String numero,

        @Size(max = 50, message = "Complemento deve ter no máximo 50 caracteres")
        String complemento,

        @NotBlank(message = "Bairro é obrigatório")
        @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
        String bairro,

        @NotBlank(message = "Cidade é obrigatória")
        @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
        String cidade,

        @NotBlank(message = "Estado é obrigatório")
        @Pattern(regexp = "^[A-Z]{2}$", message = "Estado deve ser a sigla da UF com 2 letras maiúsculas")
        String estado,

        @NotNull(message = "Principal é obrigatório")
        Boolean principal
) {
}
