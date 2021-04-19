# 配置中心
路径classpath:/configs下
dev文件夹下是开发环境目录
对应各个微服务的bootstrap.yml里的profile: dev
默认以各个服务的{spring.application.name}.yml
暂时放在本地，有条件者可将配置文件放在git
随机端口号，支持启动多个实例

### 添加pom依赖
```xml
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-config-server</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>
	</dependencies>
```

### 编写启动类
```java
@EnableConfigServer
@EnableDiscoveryClient
@SpringBootApplication
```

### 修改配置
```yml
spring:
  application:
    name: config-center
  profiles:
    active: native
  cloud:
    config:
      server:
        native:
          searchLocations: classpath:/configs/{profile}
#          searchLocations: file:/d:/configs/{profile}
        git:
          uri: https://gitee.com/zhang.w/cloud-service-configs.git
          default-label: master
          force-pull: true
          searchPaths: '{profile}'
server:
  port: 0
eureka:
  client:
    serviceUrl:
      defaultZone: http://local.register.com:8761/eureka/
    registry-fetch-interval-seconds: 5
  instance:
    lease-expiration-duration-in-seconds: 15
    lease-renewal-interval-in-seconds: 5
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${random.int}
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
logging:
  level:
    root: info
  file: logs/${spring.application.name}.log
```
