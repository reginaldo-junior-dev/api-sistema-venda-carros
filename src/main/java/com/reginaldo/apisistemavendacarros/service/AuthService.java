package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.login.LoginRequest;
import com.reginaldo.apisistemavendacarros.dto.login.LoginResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.exception.MuitasTentativasException;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import com.reginaldo.apisistemavendacarros.security.LimiteTentativas;
import com.reginaldo.apisistemavendacarros.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final LimiteTentativas limiteTentativas;

    public LoginResponse login (LoginRequest request) {

        // Por e-mail, e não por IP: no Render todas as requisições chegam pelo mesmo proxy
        String chave = "login:" + (request.email() == null ? "" : request.email().trim().toLowerCase(Locale.ROOT));
        if (limiteTentativas.bloqueado(chave)) {
            throw new MuitasTentativasException("Muitas tentativas de login com este e-mail. Aguarde alguns minutos e tente de novo.");
        }

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(request.email(), request.senha());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(token);
        } catch (AuthenticationException e) {
            limiteTentativas.registrarFalha(chave);
            throw e;
        }
        limiteTentativas.limpar(chave);

        UsuarioDetails usuarioDetails =
                (UsuarioDetails) authentication.getPrincipal();

        Usuario usuario =usuarioDetails.getUsuario();


        String jwt = jwtService.gerarToken(usuario);
        return new LoginResponse(jwt);
    }

}
