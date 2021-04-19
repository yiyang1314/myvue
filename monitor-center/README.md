# 监控中心
默认端口号9001

* 1.pom依赖
* 2.启动类
* 3.bootstrap.yml配置

## 1 pom依赖
```xml
    <dependencies>
        <dependency>
            <groupId>de.codecentric</groupId>
            <artifactId>spring-boot-admin-starter-server</artifactId>
            <version>${monitor.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
    </dependencies>
```

## 2 启动类
```java
@EnableAdminServer
@EnableDiscoveryClient
@SpringBootApplication
public class MonitorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorApplication.class, args);
    }

}
```

## 3  bachend.yml配置
```yml
spring:
  application:
    name: monitor-server
eureka:
  client:
    serviceUrl:
      defaultZone: http://local.register.com:8761/eureka/
    registry-fetch-interval-seconds: 5
  instance:
    lease-expiration-duration-in-seconds: 15
    lease-renewal-interval-in-seconds: 5
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
server:
  port: 9001
management:
  endpoints:
    web:
      exposure:
        include: "*"
  endpoint:
    health:
      show-details: always
```

