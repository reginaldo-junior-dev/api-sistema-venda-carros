package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.imagem.ImagemCarroResponse;
import com.reginaldo.apisistemavendacarros.service.ImagemCarroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/carro")
@RequiredArgsConstructor

@Tag(name = "Imagens")
public class ImagemController {

    private final ImagemCarroService imagemCarroService;

    @PostMapping(value = "/{id}/imagens", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "[Admin] Enviar imagem do carro")
    public ResponseEntity<ImagemCarroResponse> salvar (
            @PathVariable("id") UUID carroId, @RequestPart MultipartFile arquivo) throws IOException {

        ImagemCarroResponse imagemCarroResponse = imagemCarroService.salvar(carroId, arquivo);
        return ResponseEntity.ok(imagemCarroResponse);
    }

    @GetMapping("/imagens/{imagemId}/url")
    @Operation(summary = "Gerar URL temporária da imagem")
    @SecurityRequirements
    public ResponseEntity<String> gerarUrl (@PathVariable("imagemId") UUID imagemId) {
        String url = imagemCarroService.gerarUrl(imagemId);
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/imagens/{imagemId}")
    @Operation(summary = "[Admin] Excluir imagem")
    public ResponseEntity<Void> excluir(
            @PathVariable UUID imagemId) {

        imagemCarroService.excluir(imagemId);

        return ResponseEntity.noContent().build();
    }
}
