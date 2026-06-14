FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/alert-service-1.0.0.jar app.jar
EXPOSE 8083
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -XX:+UseStringDeduplication"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
