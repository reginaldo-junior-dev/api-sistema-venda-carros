package com.reginaldo.apisistemavendacarros.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    public String upload (MultipartFile arquivo) throws IOException {

        String nomeArquivo = UUID.randomUUID() + "_" + arquivo.getOriginalFilename();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket("sistema-venda-carros-imagens-reginaldo")
                .key(nomeArquivo)
                .contentType(arquivo.getContentType())
                .build();

        s3Client.putObject(
          request,
                RequestBody.fromInputStream(
                        arquivo.getInputStream(),
                        arquivo.getSize()
                )
        );

        return nomeArquivo;
    }

    public String gerarUrl(String nomeArquivo) {

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket("sistema-venda-carros-imagens-reginaldo")
                .key(nomeArquivo)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    public void excluir(String chaveArquivo) {

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket("sistema-venda-carros-imagens-reginaldo")
                .key(chaveArquivo)
                .build();

        s3Client.deleteObject(request);
    }


}
