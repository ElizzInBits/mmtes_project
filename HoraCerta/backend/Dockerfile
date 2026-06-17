# Etapa 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Baixa dependências primeiro (cache de layer)
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etapa 2: Runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copia o jar gerado
COPY --from=build /app/target/*.jar app.jar

# Copia as credenciais do Google Calendar
COPY src/main/resources/google-credentials.json /app/google-credentials.json

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
