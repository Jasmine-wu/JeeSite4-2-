FROM frolvlad/alpine-java:jdk8-slim
MAINTAINER ThinkGem@163.com
ENV TZ "Asia/Shanghai"
ENV LANG C.UTF-8
VOLUME /tmp

WORKDIR /app
# add文件到/app目录
ADD web.war .
# 解压web.war
RUN jar -xvf web.war

WORKDIR /app/WEB-INF
# 添加java启动参数
ENV JAVA_OPTS "-Xms256m -Xmx1024m -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=512m"
# 添加profiles.active=prod，启动的配置文件就会加载后缀是-prod配置文件
ENV JAVA_OPTS "$JAVA_OPTS -Dspring.profiles.active=prod"
# 添加启动的-prod配置文件
ADD application-prod.yml ./classes/config

# 启动spring-boot项目
ENTRYPOINT java -cp /app $JAVA_OPTS org.springframework.boot.loader.WarLauncher

# 暴露sprint-boot的端口
EXPOSE 8980

