# 使用官方的 OpenJDK 作为基础镜像
FROM openjdk:17-jdk-slim AS build

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 文件
COPY pom.xml .

# 复制 src 目录
COPY src ./src

# 使用 Maven 构建项目
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# 创建运行镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制生成的 JAR 文件
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar /app/demo.jar

# 设置 PORT 环境变量，Render 平台要求
ENV PORT 8080

# 开放应用程序运行的端口
EXPOSE 8080

# 执行应用程序
ENTRYPOINT ["java", "-jar", "/app/demo.jar"]
