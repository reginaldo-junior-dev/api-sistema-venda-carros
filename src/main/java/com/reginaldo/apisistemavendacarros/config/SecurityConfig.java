package com.reginaldo.apisistemavendacarros.config;

import com.reginaldo.apisistemavendacarros.security.JwtAuthenticationFilter;
import com.reginaldo.apisistemavendacarros.security.OAuth2LoginSucessoHandler;
import com.reginaldo.apisistemavendacarros.security.UsuarioDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final OAuth2LoginSucessoHandler oAuth2LoginSucessoHandler;
    private final UsuarioDetailsService usuarioDetailsService;

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
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/usuario").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/carro", "/carro/**",
                                "/marca", "/marca/**",
                                "/modelo", "/modelo/**",
                                "/categoria", "/categoria/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/carro", "/marca", "/modelo", "/categoria"
                        ).hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT,
                                "/carro/**", "/marca/**", "/modelo/**", "/categoria/**"
                        ).hasRole("ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE,
                                "/carro/**", "/marca/**", "/modelo/**", "/categoria/**"
                        ).hasRole("ADMINISTRADOR")
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
