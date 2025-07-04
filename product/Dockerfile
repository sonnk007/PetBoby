# ---------- Stage 1: Build Spring Boot app ----------
FROM maven:3.8.8-amazoncorretto-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- Stage 2: Run app ----------
FROM amazoncorretto:17
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
