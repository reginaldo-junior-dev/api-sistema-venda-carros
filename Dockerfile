# Imagem da API: usada no deploy do Render e no docker compose deste repositório

# 1) Build com Maven. As dependências ficam numa camada própria e só baixam de novo quando o pom.xml muda
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
# Os testes rodam no CI (precisam de PostgreSQL); aqui só empacota
RUN mvn -B -q package -DskipTests

# 2) Execução só com o JRE, sem Maven nem código-fonte
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home api
COPY --from=build /app/target/*.jar app.jar
USER api
EXPOSE 8080
# Usa até 75% da memória do container (o plano gratuito do Render tem 512 MB)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["java", "-jar", "app.jar"]
