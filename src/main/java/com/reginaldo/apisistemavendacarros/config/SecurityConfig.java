package com.reginaldo.apisistemavendacarros.config;

import com.reginaldo.apisistemavendacarros.security.CsrfCookieFilter;
import com.reginaldo.apisistemavendacarros.security.JwtAuthenticationFilter;
import com.reginaldo.apisistemavendacarros.security.OAuth2LoginFalhaHandler;
import com.reginaldo.apisistemavendacarros.security.OAuth2LoginSucessoHandler;
import com.reginaldo.apisistemavendacarros.security.RespostaErroSeguranca;
import com.reginaldo.apisistemavendacarros.security.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSucessoHandler oAuth2LoginSucessoHandler;
    private final OAuth2LoginFalhaHandler oAuth2LoginFalhaHandler;
    private final UsuarioDetailsService usuarioDetailsService;
    private final RespostaErroSeguranca respostaErroSeguranca;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                // A sessão vai num cookie, que o navegador manda sozinho: sem isso, outro site poderia agir em nome da pessoa.
                // spa(): token no cookie XSRF-TOKEN, devolvido pelo front no cabeçalho X-XSRF-TOKEN.
                // Ficam de fora o webhook (sem cookie, validado pela assinatura da Stripe) e quem se autentica
                // pelo cabeçalho Authorization, que outro site não consegue enviar
                .csrf(csrf -> csrf.spa()
                        .ignoringRequestMatchers("/stripe/webhook")
                        .ignoringRequestMatchers(request -> request.getHeader(HttpHeaders.AUTHORIZATION) != null)
                )
                .addFilterAfter(new CsrfCookieFilter(), CsrfFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2LoginSucessoHandler)
                        .failureHandler(oAuth2LoginFalhaHandler)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(respostaErroSeguranca.naoAutenticado())
                        .accessDeniedHandler(respostaErroSeguranca.acessoNegado())
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Chamado pela Stripe, sem JWT: a origem é validada pelo header Stripe-Signature
                        .requestMatchers(HttpMethod.POST, "/stripe/webhook").permitAll()
                        .requestMatchers(HttpMethod.GET, "/carro/imagens/*/url").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/logout").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuario").permitAll()
                        .requestMatchers("/usuario/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/usuario", "/usuario/*").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/usuario/*").hasRole("ADMINISTRADOR")
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
                        .requestMatchers(HttpMethod.POST, "/pagamento/cartao").authenticated()
                        .requestMatchers(HttpMethod.GET, "/cliente/me/pagamentos").authenticated()
                        // O ParcelaService verifica se é admin ou dono da compra
                        .requestMatchers(HttpMethod.GET, "/pagamento/*/parcelas", "/parcela/*").authenticated()
                        .requestMatchers("/pagamento", "/pagamento/**").hasRole("ADMINISTRADOR")
                        .requestMatchers("/parcela", "/parcela/**").hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET,
                                "/carro", "/carro/**",
                                "/marca", "/marca/**",
                                "/modelo", "/modelo/**",
                                "/categoria", "/categoria/**",
                                "/cor", "/cor/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/carro", "/marca", "/modelo", "/categoria", "/cor",
                                "/carro/*/imagens"
                        ).hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT,
                                "/carro/**", "/marca/**", "/modelo/**", "/categoria/**", "/cor/**"
                        ).hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE,
                                "/carro/**", "/marca/**", "/modelo/**", "/categoria/**", "/cor/**"
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
