FROM eclipse-temurin:25-jre-alpine
RUN mkdir /opt/app/
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
RUN chown appuser /opt/app/
USER appuser
COPY target/*.jar /opt/app/app.jar
COPY entrypoint.sh /opt/app/entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["sh", "/opt/app/entrypoint.sh"]