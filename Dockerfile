# --- Etapa 1: Construcción con Maven ---
# Usamos una imagen oficial de Maven con la versión de Java 17
FROM maven:3.9-eclipse-temurin-17-focal AS builder

# Establecemos el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos primero el pom.xml para aprovechar la caché de Docker
# Si las dependencias no cambian, Docker no las volverá a descargar
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el resto del código fuente
COPY src ./src

# Construimos la aplicación, omitiendo los tests para un build más rápido
RUN mvn package -DskipTests


# --- Etapa 2: Creación de la imagen final ---
# Usamos una imagen base de OpenJDK 17 mucho más ligera
FROM openjdk:17-slim

# Establecemos el directorio de trabajo
WORKDIR /app

# Copiamos el archivo .jar construido en la etapa anterior
# El nombre del .jar se basa en el artifactId y version de tu pom.xml
COPY --from=builder /app/target/prisma-0.0.1-SNAPSHOT.jar app.jar

# Expone el puerto en el que corre tu aplicación Spring (por defecto 8080)
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]