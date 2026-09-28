package com.reginaldo.apisistemavendacarros.service;

import com.reginaldo.apisistemavendacarros.dto.LoginRequest;
import com.reginaldo.apisistemavendacarros.dto.LoginResponse;
import com.reginaldo.apisistemavendacarros.entity.Usuario;
import com.reginaldo.apisistemavendacarros.security.JwtService;
import com.reginaldo.apisistemavendacarros.security.UsuarioDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login (LoginRequest request) {

        UsernamePasswordAuthenticationToken token =
                new UsernamePasswordAuthenticationToken(request.email(), request.senha());

           Authentication authentication = authenticationManager.authenticate(token);

        UsuarioDetails usuarioDetails =
                (UsuarioDetails) authentication.getPrincipal();

        Usuario usuario =usuarioDetails.getUsuario();


        String jwt = jwtService.gerarToken(usuario);
        return new LoginResponse(jwt);
    }

}
