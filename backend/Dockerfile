# 后端多阶段构建：Maven 编译 -> JRE 运行
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# 先复制 pom 单独下载依赖，利用 Docker 缓存加速
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
# 镜像构建时跳过测试（测试在 CI 阶段单独跑，避免每次打包都等待）
RUN mvn -B package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

# curl 用于 compose 的 healthcheck 与运维探活；装完清理 apt 缓存减小镜像
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# 以非 root 用户运行，降低容器逃逸风险
RUN useradd -r -u 1001 -m appuser
COPY --from=build /app/target/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser

EXPOSE 8080

# 容器内不需要 JDK 的堆外调试能力，限制堆大小避免占满宿主机
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Shanghai"

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
