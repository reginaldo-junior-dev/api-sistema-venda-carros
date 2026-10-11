package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.enums.FormatoImagem;
import org.springframework.beans.factory.annotation.Value;
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
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner, @Value("${aws.s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    // O formato já foi conferido pelos bytes do arquivo. A chave não usa o nome original, que vem de quem envia
    public String upload (MultipartFile arquivo, FormatoImagem formato) throws IOException {

        String nomeArquivo = UUID.randomUUID() + "." + formato.getExtensao();

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(nomeArquivo)
                .contentType(formato.getContentType())
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
                .bucket(bucket)
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
                .bucket(bucket)
                .key(chaveArquivo)
                .build();

        s3Client.deleteObject(request);
    }
}
