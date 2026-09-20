
FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY omnishift-runtime-spring/target/omnishift-runtime.jar app.jar

ENTRYPOINT ["java","-jar","app.jar"]
