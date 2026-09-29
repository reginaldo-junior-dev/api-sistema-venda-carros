package com.reginaldo.apisistemavendacarros.controller;

import com.reginaldo.apisistemavendacarros.dto.ImagemCarroResponse;
import com.reginaldo.apisistemavendacarros.service.ImagemCarroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/carro")
@RequiredArgsConstructor

public class ImagemController {

    private final ImagemCarroService imagemCarroService;

    @PostMapping("/{id}/imagens")
    public ResponseEntity<ImagemCarroResponse> salvar (
            @PathVariable("id") UUID carroId, @RequestParam MultipartFile arquivo) throws IOException {

        ImagemCarroResponse imagemCarroResponse = imagemCarroService.salvar(carroId, arquivo);
        return ResponseEntity.ok(imagemCarroResponse);
    }

    @GetMapping("/imagens/{imagemId}/url")
    public ResponseEntity<String> gerarUrl (@PathVariable("imagemId") UUID imagemId) {
        String url = imagemCarroService.gerarUrl(imagemId);
        return ResponseEntity.ok(url);
    }

    @DeleteMapping("/imagens/{imagemId}")
    public ResponseEntity<Void> excluir(
            @PathVariable UUID imagemId) {

        imagemCarroService.excluir(imagemId);

        return ResponseEntity.noContent().build();
    }
}
