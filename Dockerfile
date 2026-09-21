# Estágio de build: compila o reactor Maven completo dentro do container. Evita depender
# de um jar pré-construído no host (o Dockerfile anterior esperava isso, sem nunca rodar
# `mvn` de verdade) e roda em Linux, então não sofre da instabilidade conhecida do
# protobuf-maven-plugin com o Windows Defender que aparece em builds no Windows.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY . .
RUN mvn -q -f pom.xml install -DskipTests

# Estágio final: só o JRE (não o JDK completo usado pra compilar) numa imagem enxuta,
# rodando como usuário não-root.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S omnishift && adduser -S omnishift -G omnishift
WORKDIR /app
COPY --from=build /workspace/omnishift-runtime-spring/target/omnishift-runtime.jar app.jar
USER omnishift

EXPOSE 8080 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=20s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java","-jar","app.jar"]
