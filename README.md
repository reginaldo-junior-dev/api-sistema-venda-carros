# Pátio · API

[![CI](https://github.com/reginaldo-junior-dev/api-sistema-venda-carros/actions/workflows/ci.yml/badge.svg)](https://github.com/reginaldo-junior-dev/api-sistema-venda-carros/actions/workflows/ci.yml)
![Java 21](https://img.shields.io/badge/Java-21-orange)
![Spring Boot 4](https://img.shields.io/badge/Spring%20Boot-4.1-6db33f)
![PostgreSQL 16](https://img.shields.io/badge/PostgreSQL-16-336791)

Projeto de portfólio full stack que simula o sistema de uma revenda de carros novos e seminovos, a **Pátio**: catálogo público, área do cliente com reserva e pagamento online, e painel para a equipe da revenda. Os pagamentos com cartão rodam no modo de testes da Stripe.

Este repositório é a **API REST** (Java + Spring Boot). O site que a consome está no repositório [frontend-sistema-venda-carros](https://github.com/reginaldo-junior-dev/frontend-sistema-venda-carros) (React).

---

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Funcionalidades](#funcionalidades)
- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Decisões técnicas e de segurança](#decisões-técnicas-e-de-segurança)
- [Diagramas](#diagramas)
- [Como rodar](#como-rodar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Documentação da API (Swagger)](#documentação-da-api-swagger)
- [Pagamentos de teste](#pagamentos-de-teste)
- [Login com Google](#login-com-google)
- [Acesso de administrador](#acesso-de-administrador)
- [Testes](#testes)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Deploy no Render](#deploy-no-render)
- [Escopo e próximos passos](#escopo-e-próximos-passos)

---

## Sobre o projeto

A API cuida de todo o ciclo de venda de um carro:

1. A equipe da revenda cadastra o estoque (carros, fotos, marcas, modelos, cores e categorias).
2. O visitante navega pelo catálogo com filtros, sem precisar de conta.
3. O cliente cria a conta (e-mail e senha ou Google), completa os dados de comprador e **reserva** o carro: ele sai da vitrine por 30 minutos enquanto a pessoa paga.
4. O pagamento é feito com **cartão** (Stripe, inclusive com a confirmação do banco, o 3D Secure) ou por **Pix/boleto**, que a equipe confirma à mão e pode dividir em parcelas.
5. Pagamento aprovado: a compra é aprovada, o carro passa a VENDIDO e o cliente recebe um e-mail. Sem pagamento em 30 minutos, a reserva expira e o carro volta para a vitrine.

O foco do projeto está no back-end: regras de negócio consistentes mesmo com acessos simultâneos, integração de pagamentos resistente a falhas e segurança da sessão.

---

## Funcionalidades

**Visitante (sem login)**
- Catálogo paginado com busca por nome e filtros: marca, modelo, categoria, cor, condição, combustível, câmbio, status, faixa de preço, faixa de ano e quilometragem máxima.
- Detalhes do carro com fotos (URL temporária do S3).
- Criar conta com e-mail e senha ou entrar com Google.

**Usuário logado**
- Ver, editar e excluir a própria conta. Trocar e-mail ou senha exige a senha atual.
- Sair (encerra a sessão em todos os aparelhos).
- Completar o cadastro de cliente (CPF, data de nascimento e telefone), necessário para comprar.

**Cliente**
- Endereços (um deles marcado como principal).
- Favoritos e "tenho interesse" em um carro.
- Comprar (reservar) um carro e pagar com cartão, Pix ou boleto.
- Ver as próprias compras, pagamentos e parcelas.

**Administrador**
- Cadastro de carros, fotos, marcas, modelos, cores e categorias.
- Consulta e exclusão de usuários e clientes.
- Quadro de interesses dos clientes, com status de atendimento.
- Vendas: cancelar reservas, aprovar ou recusar Pix e boleto, gerar parcelas (1 a 12) e dar baixa nelas.

**Automações**
- **Expiração de reservas** (a cada 1 min): compra pendente há 30 minutos sem Pix/boleto aguardando confirmação é cancelada, a tentativa de cartão aberta é cancelada na Stripe, o carro volta a DISPONIVEL e o cliente recebe um e-mail.
- **Reconciliação com a Stripe** (a cada 1 min): pagamentos com cartão ainda pendentes são conferidos direto na Stripe, caso o webhook não chegue.
- **E-mails** (boas-vindas, compra aprovada, compra expirada) em HTML, enviados só depois que a operação foi salva no banco.

---

## Tecnologias

| Área | Ferramenta |
|---|---|
| Linguagem e framework | Java 21, Spring Boot 4.1 (Web MVC, Validation) |
| Banco de dados | PostgreSQL 16, Spring Data JPA / Hibernate, Flyway (migrações) |
| Segurança | Spring Security, JWT ([java-jwt](https://github.com/auth0/java-jwt)), OAuth2 Login (Google), BCrypt |
| Pagamentos | [Stripe](https://docs.stripe.com/api) (PaymentIntents + webhooks) |
| Arquivos | AWS S3 (SDK v2, URLs pré-assinadas) |
| E-mail | Spring Mail + templates Thymeleaf |
| Mapeamento | MapStruct + Lombok |
| Documentação | springdoc-openapi (Swagger UI) |
| Testes | JUnit 5, MockMvc, Mockito, GreenMail, PostgreSQL real |
| Infra | Docker (multi-stage), Docker Compose, GitHub Actions, Render |

---

## Arquitetura

![Diagrama de arquitetura](docs/diagramas/Diagrama%20de%20arquitetura.png)

A API segue camadas: **controllers** recebem e validam os DTOs, **services** concentram as regras de negócio e as transações, e **repositories** acessam o banco. Os filtros do catálogo são montados com JPA Specifications. Erros de qualquer camada saem no mesmo formato JSON, pelo tratador global de exceções.

O front-end chama a API pelo próprio domínio (`/api`), e a Vercel repassa as chamadas para o Render. Assim, o cookie da sessão pertence ao site e funciona em todos os navegadores, inclusive os que bloqueiam cookies de terceiros.

---

## Decisões técnicas e de segurança

**Sessão e autenticação**
- O JWT fica num cookie `HttpOnly`, `Secure` e `SameSite=Lax`: nenhum JavaScript da página consegue ler o token, nem um script injetado (XSS). A resposta do login nunca traz o token no corpo.
- Proteção **CSRF** no modo SPA: o front devolve o token do cookie `XSRF-TOKEN` no cabeçalho `X-XSRF-TOKEN`. Ficam de fora o webhook da Stripe (validado pela assinatura) e quem se autentica pelo cabeçalho `Authorization`, que outro site não consegue enviar.
- **Revogação de sessão**: o token carrega a versão da sessão do usuário. Sair, trocar a senha ou o login Google assumir a conta incrementa essa versão, e todos os tokens antigos deixam de valer, mesmo os copiados.
- O **perfil é lido do banco a cada requisição**: rebaixar um administrador vale na hora, sem esperar o token vencer.
- A API não sobe com um `JWT_SECRET` com menos de 32 caracteres, porque com um segredo curto daria para forjar tokens.
- **Limite de tentativas de login**: 5 erros por e-mail e IP, ou 20 por e-mail somando todos os IPs, bloqueiam por 15 minutos. Quem erra a senha de outra pessoa bloqueia só a si mesmo. O IP vem do `X-Forwarded-For` do proxy, lido de um jeito que ignora valores forjados pelo cliente.
- **Login com Google** só entra com e-mail confirmado pelo Google. Se já existir uma conta criada com senha para aquele e-mail, a senha é descartada e as sessões abertas com ela caem. Isso impede alguém de cadastrar o e-mail de outra pessoa antes dela e espiar a conta depois.
- Administradores não são criados, promovidos nem excluídos pela API, então o sistema nunca fica sem administrador.

**Compras e pagamentos**
- **Bloqueio pessimista** (`SELECT ... FOR UPDATE`) em carro, compra e pagamento: duas pessoas não conseguem reservar o mesmo carro, nem duas abas criar dois pagamentos para a mesma compra. Os bloqueios seguem sempre a mesma ordem, para evitar deadlock.
- O preço é **copiado para a compra**: mudar o preço do carro depois não altera vendas já feitas.
- O número do cartão **nunca passa pela API**: o front o envia direto para a Stripe e recebe só um `paymentMethodId`.
- Cada PaymentIntent é criado com **chave de idempotência** (o id do pagamento): uma requisição repetida não cobra duas vezes.
- O **webhook** confere a assinatura e recusa eventos com mais de 5 minutos. Como os eventos podem chegar fora de ordem, ele consulta o status atual na Stripe antes de aplicar. Eventos repetidos são ignorados.
- Uma nova tentativa de cartão **cancela a anterior na Stripe**: o cliente não consegue concluir um 3D Secure antigo e ser cobrado duas vezes.
- Pagamento com cartão **só é aprovado pela Stripe**, nunca à mão. Parcelas existem só para Pix e boleto.
- Carro VENDIDO não pode ser editado, excluído nem ter fotos alteradas: ele faz parte do histórico da venda.

**Consistência**
- E-mails e a exclusão de fotos no S3 acontecem **depois do commit**: se o banco falhar, nenhum e-mail sai e nenhuma foto some.
- Fotos aceitas só em **JPEG, PNG e WebP**, conferidos pelos primeiros bytes do arquivo e não pelo tipo que o navegador informa. O nome original do arquivo não é usado na chave do S3.
- O esquema do banco é criado só pelo Flyway, e o Hibernate apenas valida (`ddl-auto=validate`).

---

## Diagramas

### Casos de uso

O que cada perfil (visitante, usuário logado, cliente e administrador) pode fazer, e quais ações acionam serviços externos (Google, Stripe, S3, SMTP) ou as tarefas agendadas.

![Diagrama de casos de uso](docs/diagramas/Diagrama%20de%20caso%20de%20uso.png)

### Modelo de dados (DER)

As tabelas criadas pelas migrações do Flyway, com chaves, restrições de unicidade e cardinalidades.

![Diagrama entidade-relacionamento](docs/diagramas/Diagrama%20entidade-relacionamento.png)

### Estados de compra, carro, pagamento e parcela

As transições permitidas de cada status e o que dispara cada uma.

![Diagrama de estados](docs/diagramas/Diagramas%20de%20estado.png)

### Sequência: compra e pagamento com cartão

O caminho completo de uma compra com cartão: reserva com bloqueio, criação do PaymentIntent, 3D Secure, webhook, reconciliação e expiração.

![Diagrama de sequência](docs/diagramas/Diagrama%20de%20sequ%C3%AAncia.png)

---

## Como rodar

Há dois jeitos: com **Docker** (mais simples, não precisa instalar Java nem PostgreSQL) ou **direto na máquina**.

### Opção 1: Docker

Pré-requisito: [Docker Desktop](https://www.docker.com/products/docker-desktop/).

1. **Clone o repositório**

   ```bash
   git clone https://github.com/reginaldo-junior-dev/api-sistema-venda-carros.git
   cd api-sistema-venda-carros
   ```

2. **Crie o arquivo `.env`** na raiz do projeto (ele não vai para o Git). Só o `JWT_SECRET` é obrigatório:

   ```env
   # Obrigatório: no mínimo 32 caracteres. Gere um com: openssl rand -base64 48
   JWT_SECRET=troque-por-um-texto-longo-e-aleatorio-com-32-ou-mais-caracteres
   ```

   Sem as outras variáveis a API sobe normalmente, mas o login com Google, o cartão, as fotos e os e-mails não funcionam. Para ativá-los, acrescente as variáveis da seção [Variáveis de ambiente](#variáveis-de-ambiente).

3. **Suba o banco e a API**

   ```bash
   docker compose up --build
   ```

   O Flyway cria as tabelas na primeira subida. A API fica em **http://localhost:8080** e a documentação em **http://localhost:8080/swagger-ui.html**.

Para parar: `Ctrl + C`. Os dados do banco continuam salvos entre uma subida e outra; `docker compose down -v` apaga tudo.

### Opção 2: direto na máquina

Pré-requisitos: **Java 21** e **PostgreSQL 16**. O Maven não precisa ser instalado: o projeto traz o Maven Wrapper (`mvnw`).

1. **Crie o banco**

   ```sql
   CREATE DATABASE sistema_venda_carros;
   ```

2. **Crie o arquivo `.env`** na raiz do projeto. Rodando direto na máquina, todas as variáveis abaixo precisam existir. As que você não for usar podem ficar com um valor qualquer, como `nao-configurado`:

   ```env
   VENDA_CARROS_DB_URL=jdbc:postgresql://localhost:5432/sistema_venda_carros
   VENDA_CARROS_DB_USERNAME=postgres
   VENDA_CARROS_DB_PASSWORD=sua-senha

   JWT_SECRET=troque-por-um-texto-longo-e-aleatorio-com-32-ou-mais-caracteres

   GOOGLE_CLIENT_ID=nao-configurado
   GOOGLE_CLIENT_SECRET=nao-configurado

   STRIPE_SECRET_KEY=nao-configurado
   STRIPE_WEBHOOK_SECRET=nao-configurado

   AWS_ACCESS_KEY_ID=
   AWS_SECRET_ACCESS_KEY=

   MAIL_USERNAME=
   MAIL_PASSWORD=
   ```

3. **Suba a API**

   ```bash
   # Linux / macOS / Git Bash
   ./mvnw spring-boot:run

   # Windows (PowerShell ou CMD)
   mvnw.cmd spring-boot:run
   ```

   Para ver no console o SQL que o Hibernate executa, ative o perfil de desenvolvimento: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`. No IntelliJ, o mesmo vai em *Run → Edit Configurations → Active profiles*.

### Subir o site junto

Com a API rodando, siga o README do [front-end](https://github.com/reginaldo-junior-dev/frontend-sistema-venda-carros). O site abre em `http://localhost:5173` e repassa as chamadas de `/api` para a API em `http://localhost:8080`.

---

## Variáveis de ambiente

O arquivo `.env` da raiz é lido automaticamente pela aplicação e pelo Docker Compose.

| Variável | Obrigatória | Para que serve |
|---|---|---|
| `VENDA_CARROS_DB_URL` | Sim (fora do Docker) | URL JDBC do PostgreSQL, como `jdbc:postgresql://localhost:5432/sistema_venda_carros`. No Docker Compose, já aponta para o container do banco. |
| `VENDA_CARROS_DB_USERNAME` | Sim (fora do Docker) | Usuário do banco. |
| `VENDA_CARROS_DB_PASSWORD` | Sim (fora do Docker) | Senha do banco. |
| `JWT_SECRET` | Sim | Assina os tokens de login. No mínimo 32 caracteres. Trocar o valor desloga todo mundo. |
| `FRONTEND_URL` | Não | Endereço do site, sem barra no final (padrão: `http://localhost:5173`). Usado na volta do login com Google. |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Para o login Google | Credenciais OAuth do [Google Cloud](https://console.cloud.google.com/apis/credentials). Veja [Login com Google](#login-com-google). |
| `STRIPE_SECRET_KEY` | Para o cartão | Chave secreta da Stripe (`sk_test_...`). |
| `STRIPE_WEBHOOK_SECRET` | Para o cartão | Segredo do webhook (`whsec_...`). Veja [Pagamentos de teste](#pagamentos-de-teste). |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Para as fotos | Chave de um usuário IAM com acesso ao bucket. Vazias, a API usa a cadeia padrão da AWS (ex.: IAM Role). |
| `AWS_REGION` | Não | Região do bucket (padrão: `sa-east-1`). |
| `AWS_S3_BUCKET` | Não | Nome do bucket das fotos. |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | Para os e-mails | Credenciais do SMTP. O padrão é o sandbox do [Mailtrap](https://mailtrap.io), onde os e-mails ficam numa caixa de testes e não chegam de verdade. |
| `MAIL_HOST` / `MAIL_PORT` | Não | Servidor SMTP (padrão: `sandbox.smtp.mailtrap.io:2525`). |
| `MAIL_REMETENTE` | Não | Remetente dos e-mails (padrão: `no-reply@vendacarros.com`). |
| `COOKIE_SEGURO` | Não | Cookie da sessão só por HTTPS (padrão: `true`). O navegador abre exceção para `http://localhost`; use `false` só para acessar por outro endereço sem HTTPS, como o IP da máquina na rede local. |

Outras configurações ficam no [`application.properties`](src/main/resources/application.properties), como o tempo da reserva (`compra.expiracao.tempo=30m`) e o limite de tentativas de login.

---

## Documentação da API (Swagger)

Com a API rodando, abra **http://localhost:8080/swagger-ui.html**. Todos os endpoints estão lá, agrupados por assunto, com os campos de cada requisição e resposta.

Para testar rotas que exigem login direto no Swagger:

1. Execute `POST /auth/login` com o e-mail e a senha de uma conta. O navegador grava o cookie da sessão.
2. As próximas requisições do Swagger já vão autenticadas, inclusive com o token CSRF.

Também é possível se autenticar por `Authorization: Bearer <token>` (útil no Postman e nos testes); nesse caso o CSRF não é exigido.

<details>
<summary><strong>Resumo dos endpoints</strong></summary>

Rotas marcadas com **[Admin]** exigem o perfil ADMINISTRADOR. As de consulta do catálogo são públicas.

| Recurso | Endpoints |
|---|---|
| Autenticação | `POST /auth/login`, `POST /auth/logout`, login Google em `/oauth2/authorization/google` |
| Conta | `POST /usuario` (criar), `GET/PUT/DELETE /usuario/me`, [Admin] `GET /usuario`, `GET/DELETE /usuario/{id}` |
| Cliente | `POST /cliente`, `GET/PUT/DELETE /cliente/me`, [Admin] `GET /cliente`, `GET/DELETE /cliente/{id}`, `GET /cliente/{id}/enderecos` |
| Endereços | `POST/GET /endereco`, `GET/PUT/DELETE /endereco/{id}` |
| Catálogo | `GET /carro` (filtros e paginação), `GET /carro/{id}`, [Admin] `POST /carro`, `PUT/DELETE /carro/{id}` |
| Fotos | `GET /carro/imagens/{id}/url`, [Admin] `POST /carro/{id}/imagens`, `DELETE /carro/imagens/{id}` |
| Marca, modelo, cor, categoria | `GET` público; [Admin] `POST`, `PUT /{id}`, `DELETE /{id}` |
| Favoritos | `POST/DELETE /carro/{id}/favorito`, `GET /cliente/me/favoritos` |
| Interesses | `POST /carro/{id}/interesse`, `GET /cliente/me/interesses`, [Admin] `GET /interesse`, `GET /interesse/{id}`, `PUT /interesse/{id}/status` |
| Compras | `POST /compra`, `GET /cliente/me/compras`, [Admin] `GET /compra`, `GET /compra/{id}`, `PUT /compra/{id}/cancelar` |
| Pagamentos | `POST /pagamento/cartao`, `POST /pagamento` (Pix/boleto), `GET /cliente/me/pagamentos`, [Admin] `GET /pagamento`, `GET /pagamento/{id}`, `PUT /pagamento/{id}/aprovar`, `/recusar`, `/cancelar` |
| Parcelas | `GET /pagamento/{id}/parcelas`, `GET /parcela/{id}`, [Admin] `POST /pagamento/{id}/parcelas`, `PUT /parcela/{id}/pagar`, `PUT /parcela/{id}/cancelar` |
| Webhook | `POST /stripe/webhook` (chamado pela Stripe) |

As listas são paginadas: `?page=0&size=20&sort=campo,desc` (no máximo 100 itens por página).

</details>

---

## Pagamentos de teste

Com uma chave `sk_test_...`, nenhum dinheiro de verdade é cobrado. Cartões de teste da Stripe:

| Cartão | Resultado |
|---|---|
| `4242 4242 4242 4242` | Aprovado na hora |
| `4000 0027 6000 3184` | Pede a confirmação do banco (3D Secure) |
| `4000 0000 0000 9995` | Recusado por saldo insuficiente |

Use qualquer validade futura e qualquer CVC de 3 dígitos.

**Webhook em desenvolvimento.** O resultado do 3D Secure chega pelo webhook. Para recebê-lo na sua máquina, instale o [Stripe CLI](https://docs.stripe.com/stripe-cli) e deixe este comando rodando num terminal:

```bash
stripe listen --forward-to localhost:8080/stripe/webhook
```

Ele mostra um segredo `whsec_...`: coloque-o em `STRIPE_WEBHOOK_SECRET` e reinicie a API. Sem o webhook, a reconciliação confere os pagamentos pendentes na Stripe a cada minuto, então a compra é aprovada com até 1 minuto de atraso.

**Pix e boleto** ficam pendentes até um administrador aprovar o pagamento (`PUT /pagamento/{id}/aprovar`).

---

## Login com Google

1. No [Google Cloud Console](https://console.cloud.google.com/apis/credentials), crie uma credencial **ID do cliente OAuth** do tipo *Aplicativo da Web*.
2. Em **URIs de redirecionamento autorizados**, cadastre o endereço do site seguido de `/api/login/oauth2/code/google`:
   - desenvolvimento: `http://localhost:5173/api/login/oauth2/code/google`
   - produção: `https://<seu-site>.vercel.app/api/login/oauth2/code/google`
3. Coloque o ID e o segredo em `GOOGLE_CLIENT_ID` e `GOOGLE_CLIENT_SECRET`, e o endereço do site em `FRONTEND_URL`.

O retorno passa pelo site (e não direto pela API) para que o cookie da sessão seja gravado no domínio do site. Por isso o login com Google precisa do front rodando.

---

## Acesso de administrador

Todo cadastro feito pela API cria uma conta de usuário comum, e não existe rota para promover alguém. Para ter um administrador:

1. Crie uma conta normalmente (pelo site ou por `POST /usuario`).
2. Promova a conta no banco:

   ```sql
   UPDATE usuario SET perfil = 'ADMINISTRADOR' WHERE email = 'seu@email.com';
   ```

   Rodando pelo Docker, o mesmo comando vai assim:

   ```bash
   docker compose exec db psql -U venda_carros -d venda_carros -c "UPDATE usuario SET perfil = 'ADMINISTRADOR' WHERE email = 'seu@email.com';"
   ```

A mudança vale na próxima requisição, porque o perfil é lido do banco a cada chamada.

---

## Testes

São mais de 300 testes, rodando contra um **PostgreSQL de verdade** (não um banco em memória), com as mesmas migrações do Flyway da produção:

- **Controllers** (MockMvc): regras de acesso por perfil, validação, códigos de erro, CSRF e cookie da sessão.
- **Regras de negócio**: reserva, expiração, pagamentos, parcelas, exclusões em cascata.
- **Concorrência**: várias threads tentando comprar o mesmo carro ou pagar a mesma compra ao mesmo tempo.
- **Transações**: e-mail e exclusão de foto só depois do commit; se a aprovação da compra falhar, a aprovação do pagamento é desfeita junto.
- **Integrações**: Stripe e S3 simulados com Mockito; e-mails conferidos com o GreenMail.

Para rodar, a API precisa enxergar um PostgreSQL pelas variáveis `VENDA_CARROS_DB_*` do `.env` (os testes apagam o que criam):

```bash
./mvnw test
```

O [GitHub Actions](.github/workflows/ci.yml) roda todos os testes a cada push e pull request na `main`, com um PostgreSQL 16 criado só para a execução.

---

## Estrutura de pastas

```
src/main/java/com/reginaldo/apisistemavendacarros
├── config/          Segurança, Swagger, S3, Stripe, tarefas agendadas e execução assíncrona
├── controller/      Endpoints REST
├── dto/             Objetos de entrada e saída, separados por assunto
├── entity/          Entidades JPA
├── enums/           Status, perfis e tipos
├── event/           Eventos de domínio e o listener que envia os e-mails após o commit
├── exception/       Exceções de negócio e o tratador global de erros
├── mapper/          Conversão entidade ↔ DTO (MapStruct)
├── repository/      Spring Data JPA, inclusive as consultas com bloqueio
├── security/        Filtro JWT, cookie da sessão, login Google, limite de tentativas
├── service/         Regras de negócio, Stripe, S3, e-mail, expiração e reconciliação
└── specification/   Filtros dinâmicos do catálogo (JPA Specifications)

src/main/resources
├── application.properties        Configuração (lê as variáveis de ambiente)
├── application-dev.properties    Perfil de desenvolvimento (mostra o SQL no console)
├── db/migration/                 Migrações do Flyway (V1 a V10)
└── templates/email/              Templates HTML dos e-mails (Thymeleaf)
```
---

## Escopo e próximos passos

Pontos deixados de fora de propósito, para manter o foco nas compras e nos pagamentos, e como cada um seria resolvido:

- **Recuperação de senha por e-mail.** Ainda não existe. O caminho seria um token de uso único e de validade curta, guardado no banco só como hash, enviado por e-mail e invalidado depois do uso. Trocar a senha por ele já encerraria as outras sessões, pela versão da sessão que a API já usa.
- **Cadastro revela se um e-mail tem conta.** A resposta "E-mail já cadastrado" permite descobrir quem é cliente. A correção seria responder sempre a mesma mensagem e avisar por e-mail o dono da conta existente.
- **Swagger público em produção.** Ficou aberto para facilitar a avaliação do projeto. Num sistema real, ele seria desligado em produção por configuração ou liberado só para administradores.
