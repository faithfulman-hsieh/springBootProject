# 使用官方的 OpenJDK 作為基礎映像
FROM openjdk:17-jdk-slim

# 設定工作目錄
WORKDIR /app

# 複製 pom.xml 和源代码
COPY pom.xml .
COPY src ./src

# 使用 Maven 构建项目（下载依赖并打包）
RUN ./mvnw clean package -DskipTests

# 複製生成的 JAR 文件到容器中
COPY target/demo-0.0.1-SNAPSHOT.jar /app/demo.jar

# 設置 PORT 環境變數，這是 Render 平台要求的
ENV PORT 8080

# 開放應用程式運行的端口
EXPOSE 8080

# 執行應用程式
ENTRYPOINT ["java", "-jar", "/app/demo.jar"]
