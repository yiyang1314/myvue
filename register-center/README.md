# 注册中心
默认端口号8761
目前是单注册中心，多注册中心请看视频02.2章节
### 添加pom依赖
```xml
	<dependencies>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>
		</dependency>
	</dependencies>

	<build>
		<finalName>${project.artifactId}</finalName><!--打jar包去掉版本号-->
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
		</plugins>
	</build>
```

### 编写启动类
```java
@EnableEurekaServer
@SpringBootApplication
public class RegisterCenterApplication {
	public static void main(String[] args) {
		SpringApplication.run(RegisterCenterApplication.class, args);
	}
}

```

### 修改配置
```yml
spring:
  application:
    name: register-center
server:
  port: 8761
eureka:
  client:
    serviceUrl:
      defaultZone: http://local.register.com:${server.port}/eureka/
    register-with-eureka: true
    fetch-registry: false
    registry-fetch-interval-seconds: 5
  instance:
    lease-expiration-duration-in-seconds: 15
    lease-renewal-interval-in-seconds: 5
    prefer-ip-address: true
    instance-id: ${spring.application.name}:${server.port}
  server:
    enable-self-preservation: false
    eviction-interval-timer-in-ms: 3000
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