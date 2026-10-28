# ---- Etapa 1: compilación ----
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /workspace

# Dependencias primero, para aprovechar la caché de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Código fuente y empaquetado
COPY src ./src
RUN mvn package -DskipTests -q

# ---- Etapa 2: ejecución ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Usuario sin privilegios de root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

COPY --from=build /workspace/target/*.jar app.jar

EXPOSE 8080 8081
ENTRYPOINT ["java", "-jar", "app.jar"]