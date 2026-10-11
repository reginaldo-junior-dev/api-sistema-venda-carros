package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.login.LoginRequest;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.exception.MuitasTentativasException;
import com.reginaldo.apisistemavendacarros.repository.UsuarioRepository;
import com.reginaldo.apisistemavendacarros.security.LimiteTentativas;
import com.reginaldo.apisistemavendacarros.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final LimiteTentativas limiteTentativas;
    private final UsuarioRepository usuarioRepository;

    @Value("${seguranca.tentativas.maximo-por-email}")
    private int maximoPorEmail;

    // Confere e-mail e senha; o token e o cookie ficam com o AuthController.
    // ip: o do cliente, lido do X-Forwarded-For do proxy (server.forward-headers-strategy)
    public Usuario login (LoginRequest request, String ip) {

        // Por e-mail e IP: quem erra a senha de outra pessoa bloqueia só a si mesmo, não a dona da conta.
        // Por e-mail, com mais folga: segura quem troca de IP para continuar tentando senhas na mesma conta
        String email = request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT);
        String chaveEmailIp = "login:" + email + "|" + ip;
        String chaveEmail = "login:" + email;
        if (limiteTentativas.bloqueado(chaveEmailIp) || limiteTentativas.bloqueado(chaveEmail, maximoPorEmail)) {
            throw new MuitasTentativasException("Muitas tentativas de login com este e-mail. Aguarde alguns minutos e tente de novo.");
        }

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(request.email(), request.senha());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(token);
        } catch (AuthenticationException e) {
            limiteTentativas.registrarFalha(chaveEmailIp);
            limiteTentativas.registrarFalha(chaveEmail);
            throw e;
        }
        limiteTentativas.limpar(chaveEmailIp);
        limiteTentativas.limpar(chaveEmail);

        UsuarioDetails usuarioDetails =
                (UsuarioDetails) authentication.getPrincipal();

        return usuarioDetails.getUsuario();
    }

    // Recarrega do banco: o usuário da requisição está fora da transação
    @Transactional
    public void encerrarSessoes (UUID usuarioId) {
        usuarioRepository.findById(usuarioId).ifPresent(Usuario::encerrarSessoes);
    }

}
