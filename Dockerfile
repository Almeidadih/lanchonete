WORKDIR /app

# Copia o pom.xml primeiro — aproveita cache de camadas do Docker.
# As dependências só são baixadas novamente se o pom.xml mudar.
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copia o código-fonte e executa o build
COPY src ./src
RUN mvn package -DskipTests -q

# ================================================
# Stage 2: RUNTIME
# Imagem final leve com apenas o JRE (sem Maven)
# Alpine reduz o tamanho da imagem de ~600MB para ~200MB
# ================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Cria usuário não-root por segurança (boa prática em produção)
RUN addgroup -S lanchonete && adduser -S lanchonete -G lanchonete
USER lanchonete

# Copia apenas o JAR gerado no stage anterior
COPY --from=builder /app/target/*.jar app.jar

# Porta exposta pela aplicação Spring Boot
EXPOSE 8080

# Variáveis de ambiente padrão (podem ser sobrescritas no docker-compose)
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# Comando de inicialização com suporte a JAVA_OPTS
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]