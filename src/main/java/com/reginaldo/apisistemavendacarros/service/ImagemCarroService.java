package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.imagem.ImagemCarroResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.ImagemCarro;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.mapper.ImagemCarroMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ImagemCarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImagemCarroService {
    private final ImagemCarroRepository imagemCarroRepository;
    private final CarroRepository carroRepository;
    private final S3Service s3Service;
    private final ImagemCarroMapper imagemCarroMapper;

    public ImagemCarroResponse salvar(UUID carroId, MultipartFile arquivo) throws IOException {
        Carro carro = carroRepository.findById(carroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carro não encontrado"));

        String chaveArquivo = s3Service.upload(arquivo);

        boolean primeiraImagem = !imagemCarroRepository.existsByCarroId(carroId);

        int quantidadeImagens = imagemCarroRepository.countByCarroId(carroId);

        ImagemCarro imagemCarro = new ImagemCarro(
                       chaveArquivo,
                quantidadeImagens + 1,
                       primeiraImagem,
                       carro
        );

        imagemCarroRepository.save(imagemCarro);

        String url = s3Service.gerarUrl(chaveArquivo);

        return new ImagemCarroResponse(
                imagemCarro.getId(),
                imagemCarro.getChaveArquivo(),
                url,
                imagemCarro.getOrdem(),
                imagemCarro.getPrincipal(),
                carro.getId()
        );
    }

    public String gerarUrl (UUID imagemId) {
        ImagemCarro imagemCarro = imagemCarroRepository.findById(imagemId).orElseThrow(() ->
                new RecursoNaoEncontradoException("Imagem não encontrada"));

        return s3Service.gerarUrl(imagemCarro.getChaveArquivo());
    }

    public void excluir(UUID imagemId) {
        ImagemCarro imagemCarro = imagemCarroRepository.findById(imagemId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Imagem não encontrada"));

        boolean eraPrincipal = imagemCarro.getPrincipal();
        UUID carroId = imagemCarro.getCarro().getId();

        s3Service.excluir(imagemCarro.getChaveArquivo());

        imagemCarroRepository.delete(imagemCarro);

        if (eraPrincipal) {
            imagemCarroRepository.findFirstByCarroIdOrderByOrdemAsc(carroId)
                    .ifPresent(novaPrincipal -> {
                        novaPrincipal.setPrincipal(true);
                        imagemCarroRepository.save(novaPrincipal);
                    });
        }
    }
}
