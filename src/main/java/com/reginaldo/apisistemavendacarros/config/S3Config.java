package com.reginaldo.apisistemavendacarros.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    private final Region regiao;

    public S3Config(@Value("${aws.region}") String regiao) {
        this.regiao = Region.of(regiao);
    }

    // Chaves do .env (usuário IAM da API). Sem elas, usa a cadeia padrão da AWS:
    // variáveis de ambiente do sistema, ~/.aws ou IAM Role quando roda dentro da AWS
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(
            @Value("${aws.access-key-id:}") String accessKeyId,
            @Value("${aws.secret-access-key:}") String secretAccessKey) {

        if (accessKeyId.isBlank() != secretAccessKey.isBlank()) {
            throw new IllegalStateException("Informe AWS_ACCESS_KEY_ID e AWS_SECRET_ACCESS_KEY juntas");
        }

        if (accessKeyId.isBlank()) {
            return DefaultCredentialsProvider.builder().build();
        }

        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }

    @Bean
    public S3Client s3Client(AwsCredentialsProvider credenciais) {
        return S3Client.builder()
                .region(regiao)
                .credentialsProvider(credenciais)
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(AwsCredentialsProvider credenciais) {
        return S3Presigner.builder()
                .region(regiao)
                .credentialsProvider(credenciais)
                .build();
    }
}
