# ---------------------------------------------------------------- Étape 1 : compilation
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Dépendances téléchargées dans une couche à part (mise en cache tant que pom.xml ne change pas)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------------------------------------------------------------- Étape 2 : exécution (image légère)
FROM eclipse-temurin:21-jre

ENV TZ=Europe/Paris \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

WORKDIR /app
# Utilisateur sans droits administrateur
RUN groupadd --system fle && useradd --system --gid fle --no-create-home fle

COPY --from=build /build/target/fle-presence-*.jar /app/app.jar

USER fle
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
