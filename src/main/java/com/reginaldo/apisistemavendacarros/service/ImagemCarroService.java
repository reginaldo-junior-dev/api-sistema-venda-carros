package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.imagem.ImagemCarroResponse;
import com.reginaldo.apisistemavendacarros.entity.Carro;
import com.reginaldo.apisistemavendacarros.entity.ImagemCarro;
import com.reginaldo.apisistemavendacarros.enums.StatusCarro;
import com.reginaldo.apisistemavendacarros.exception.RecursoNaoEncontradoException;
import com.reginaldo.apisistemavendacarros.exception.ValorInvalidoException;
import com.reginaldo.apisistemavendacarros.mapper.ImagemCarroMapper;
import com.reginaldo.apisistemavendacarros.repository.CarroRepository;
import com.reginaldo.apisistemavendacarros.repository.ImagemCarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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

        // Antes do upload: arquivo de carro vendido nem chega ao S3
        garantirNaoVendido(carro);

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

    @Transactional
    public void excluir(UUID imagemId) {
        ImagemCarro imagemCarro = imagemCarroRepository.findById(imagemId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Imagem não encontrada"));

        garantirNaoVendido(imagemCarro.getCarro());

        boolean eraPrincipal = imagemCarro.getPrincipal();
        UUID carroId = imagemCarro.getCarro().getId();
        String chaveArquivo = imagemCarro.getChaveArquivo();

        imagemCarroRepository.delete(imagemCarro);

        if (eraPrincipal) {
            imagemCarroRepository.findFirstByCarroIdOrderByOrdemAsc(carroId)
                    .ifPresent(novaPrincipal -> {
                        novaPrincipal.setPrincipal(true);
                        imagemCarroRepository.save(novaPrincipal);
                    });
        }

        // Arquivo do S3 só é apagado depois do commit: se o banco falhar, a foto continua existindo
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3Service.excluir(chaveArquivo);
            }
        });
    }

    // A compra aponta para o carro: as fotos fazem parte do histórico da venda
    private void garantirNaoVendido(Carro carro) {
        if (carro.getStatus() == StatusCarro.VENDIDO) {
            throw new ValorInvalidoException("Fotos de carro vendido não podem ser alteradas");
        }
    }
}
