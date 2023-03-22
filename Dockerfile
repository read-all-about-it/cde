FROM openjdk:17-alpine

COPY target/uberjar/cde.jar /cde/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/cde/app.jar"]
