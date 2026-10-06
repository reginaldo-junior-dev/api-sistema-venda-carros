package com.reginaldo.apisistemavendacarros.config;

import com.reginaldo.apisistemavendacarros.security.CookieSessao;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SESSAO = "cookieSessao";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Sistema de Venda de Carros")
                        .version("1.0")
                        .description("""
                                Catálogo de carros, compra com reserva do veículo e pagamento com cartão via Stripe.

                                **Como testar:** crie uma conta em `POST /usuario` e faça login em `POST /auth/login`. \
                                A sessão fica num cookie HttpOnly que o navegador guarda sozinho: as próximas chamadas \
                                daqui já vão autenticadas. `POST /auth/logout` encerra a sessão.

                                Rotas marcadas com **[Admin]** exigem um usuário administrador. \
                                Rotas sem cadeado são públicas."""))
                .components(new Components().addSecuritySchemes(SESSAO, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name(CookieSessao.NOME)
                        .description("JWT no cookie HttpOnly gravado pelo login")))
                // Sessão em todas as rotas; as públicas removem com @SecurityRequirements vazio
                .addSecurityItem(new SecurityRequirement().addList(SESSAO))
                // Descrições ficam só aqui: @Tag com descrição no controller duplica o grupo
                .tags(List.of(
                        tag("Autenticação", "Login e logout (sessão em cookie HttpOnly)"),
                        tag("Usuários", "Conta de acesso (login)"),
                        tag("Clientes", "Dados pessoais do comprador (CPF, telefone...)"),
                        tag("Endereços", "Endereços do cliente logado"),
                        tag("Carros", "Catálogo de carros (consulta pública)"),
                        tag("Imagens", "Fotos dos carros (armazenadas no AWS S3)"),
                        tag("Marcas", null),
                        tag("Modelos", null),
                        tag("Cores", null),
                        tag("Categorias", null),
                        tag("Favoritos", "Carros favoritos do cliente logado"),
                        tag("Interesses", "Pedidos de contato sobre um carro"),
                        tag("Compras", "Criar a compra reserva o carro; sem pagamento, ela expira em 30 minutos"),
                        tag("Pagamentos", "Cartão via Stripe e pagamentos manuais"),
                        tag("Parcelas", "Parcelamento de pagamentos")));
    }

    private Tag tag(String nome, String descricao) {
        return new Tag().name(nome).description(descricao);
    }
}
