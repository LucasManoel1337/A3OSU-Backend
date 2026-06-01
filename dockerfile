# --- Etapa 1: Compilação (Build) ---
# Atualizado para o Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia o arquivo de configuração de dependências
COPY pom.xml .

# Copia todo o código-fonte do projeto
COPY src ./src

# Executa o comando do Maven para buildar o projeto pulando os testes unitários temporariamente
RUN mvn clean package -DskipTests

# --- Etapa 2: Execução (Run) ---
# Atualizado para o Java 21 JRE (mais leve, apenas para rodar)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copia o arquivo .jar gerado na Etapa 1 para dentro desta nova imagem limpa
COPY --from=build /app/target/*.jar app.jar

# Informa a porta que o container vai expor (porta padrão do Spring)
EXPOSE 8080

# Comando que inicia a aplicação assim que o container ligar
ENTRYPOINT ["java", "-jar", "app.jar"]