# ------------
# 階段 1: 建置 (Build Stage)
# ------------
# 舊的寫法 (已失效): FROM openjdk:17-jdk-slim AS build
# 新的寫法 (推薦):
FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

# 複製 Maven wrapper 和 pom.xml (利用 Docker cache 機制加速)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# 給予 mvnw 執行權限 (避免 Permission denied)
RUN chmod +x ./mvnw

# 下載依賴 (這步會比較久，但之後改 code 不改 pom 就會很快)
RUN ./mvnw dependency:go-offline -B

# 複製原始碼並打包
COPY src src
RUN ./mvnw package -DskipTests

# ------------
# 階段 2: 執行 (Runtime Stage)
# ------------
# 舊的寫法 (已失效): FROM openjdk:17-jdk-slim
# 新的寫法 (推薦):
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# 從第一階段複製打包好的 jar 檔
# 注意：這裡假設你的 jar 檔名是 app.jar 或者你需要確認 target 下的檔名
# 如果你的 pom.xml 沒有設定 finalName，通常是 artifactId-version.jar
# 這裡使用通用配對符號，或是請確認你原本的 COPY 指令
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]