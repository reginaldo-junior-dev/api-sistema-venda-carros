package com.reginaldo.apisistemavendacarros.config;

import com.reginaldo.apisistemavendacarros.security.JwtAuthenticationFilter;
import com.reginaldo.apisistemavendacarros.security.OAuth2LoginSucessoHandler;
import com.reginaldo.apisistemavendacarros.security.RespostaErroSeguranca;
import com.reginaldo.apisistemavendacarros.security.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSucessoHandler oAuth2LoginSucessoHandler;
    private final UsuarioDetailsService usuarioDetailsService;
    private final RespostaErroSeguranca respostaErroSeguranca;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .cors(cors -> {})
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .csrf(csrf -> csrf.disable())
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSucessoHandler))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(respostaErroSeguranca.naoAutenticado())
                        .accessDeniedHandler(respostaErroSeguranca.acessoNegado())
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/carro/imagens/*/url").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuario").permitAll()
                        .requestMatchers("/usuario/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuario", "/usuario/*").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/usuario/*").hasRole("ADMINISTRADOR")
                        // Precisa vir antes das regras de /carro/** para não cair na exigência de ADMINISTRADOR
                        .requestMatchers(HttpMethod.POST, "/carro/*/favorito").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/carro/*/favorito").authenticated()
                        .requestMatchers(HttpMethod.GET, "/cliente/me/favoritos").authenticated()
                        .requestMatchers(HttpMethod.POST, "/carro/*/interesse").authenticated()
                        .requestMatchers(HttpMethod.GET, "/cliente/me/interesses").authenticated()
                        .requestMatchers("/interesse", "/interesse/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/compra").authenticated()
                        .requestMatchers(HttpMethod.GET, "/cliente/me/compras").authenticated()
                        .requestMatchers("/compra", "/compra/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/pagamento").authenticated()
                        .requestMatchers(HttpMethod.GET, "/cliente/me/pagamentos").authenticated()
                        // Consultas de parcela: autenticado aqui; o ParcelaService verifica se é admin ou dono da compra
                        .requestMatchers(HttpMethod.GET, "/pagamento/*/parcelas", "/parcela/*").authenticated()
                        .requestMatchers("/pagamento", "/pagamento/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/parcela", "/parcela/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET,
                                "/carro", "/carro/**",
                                "/marca", "/marca/**",
                                "/modelo", "/modelo/**",
                                "/categoria", "/categoria/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/carro", "/marca", "/modelo", "/categoria",
                                "/carro/*/imagens"
                        ).hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT,
                                "/carro/**", "/marca/**", "/modelo/**", "/categoria/**"
                        ).hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE,
                                "/carro/**", "/marca/**", "/modelo/**", "/categoria/**"
                        ).hasRole("ADMINISTRADOR")
                        .requestMatchers("/cliente/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/cliente", "/cliente/*", "/cliente/*/enderecos").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/cliente/*").hasRole("ADMINISTRADOR")
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(usuarioDetailsService);

        provider.setPasswordEncoder(passwordEncoder);

        return new ProviderManager(provider);
    }
}
