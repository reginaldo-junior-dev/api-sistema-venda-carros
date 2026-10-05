package com.reginaldo.apisistemavendacarros.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${email.remetente}")
    private String remetente;

    // Desligado nos testes (email.ativo=false)
    @Value("${email.ativo:true}")
    private boolean ativo;

    public void enviarBoasVindas (String destinatario, String nome) {
        enviar(destinatario, "Bem-vindo ao Sistema de Venda de Carros",
                "email/boas-vindas", Map.of("nome", nome));
    }

    public void enviarCompraAprovada (String destinatario, String nome, String carro, BigDecimal valor) {
        enviar(destinatario, "Pagamento confirmado - " + carro,
                "email/compra-aprovada", Map.of("nome", nome, "carro", carro, "valor", valor));
    }

    public void enviarCompraExpirada (String destinatario, String nome, String carro, BigDecimal valor) {
        enviar(destinatario, "Sua reserva expirou - " + carro,
                "email/compra-expirada", Map.of("nome", nome, "carro", carro, "valor", valor));
    }

    // Monta o HTML a partir de templates/<template>.html.
    // Falha no envio não deve afetar a operação principal (ex.: cadastro)
    private void enviar (String destinatario, String assunto, String template, Map<String, Object> variaveis) {
        if (!ativo) {
            return;
        }

        try {
            String html = templateEngine.process(template, new Context(null, variaveis));

            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject(assunto);
            helper.setText(html, true);

            mailSender.send(mensagem);
        } catch (MailException | MessagingException e) {
            log.error("Falha ao enviar e-mail para {}: {}", destinatario, e.getMessage());
        }
    }
}
