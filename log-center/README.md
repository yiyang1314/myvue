# 日志中心

## 1 添加pom依赖
```xml
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-config</artifactId>
		</dependency>
		<dependency>
			<groupId>org.mybatis.spring.boot</groupId>
			<artifactId>mybatis-spring-boot-starter</artifactId>
		</dependency>
		<dependency>
			<groupId>mysql</groupId>
			<artifactId>mysql-connector-java</artifactId>
			<scope>runtime</scope>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-oauth2</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-amqp</artifactId>
		</dependency>
		<dependency>
			<groupId>com.cloud</groupId>
			<artifactId>commons</artifactId>
			<version>${project.version}</version>
		</dependency>
		<dependency>
			<groupId>io.springfox</groupId>
			<artifactId>springfox-swagger2</artifactId>
			<version>${swagger.version}</version>
		</dependency>
		<dependency>
			<groupId>io.springfox</groupId>
			<artifactId>springfox-bean-validators</artifactId>
			<version>${swagger.version}</version>
		</dependency>
		<dependency>
			<groupId>io.springfox</groupId>
			<artifactId>springfox-swagger-ui</artifactId>
			<version>${swagger.version}</version>
		</dependency>
		<dependency>
			<groupId>org.elasticsearch</groupId>
			<artifactId>elasticsearch</artifactId>
		</dependency>
		<dependency>
			<groupId>org.elasticsearch.client</groupId>
			<artifactId>transport</artifactId>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zipkin</artifactId>
		</dependency>
	</dependencies>
```

## 2.编写java 配置类
### 2.1 启动类注解
```java
/**启动类注解*/
@EnableDiscoveryClient
@SpringBootApplication
public class LogCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(LogCenterApplication.class, args);
	}

}
```

### 2.2 Api在线文档
```java
/** 扫描api文档接口*/
@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Bean
	public Docket docket() {
		return new Docket(DocumentationType.SWAGGER_2).groupName("认证中心swagger接口文档")
				.apiInfo(new ApiInfoBuilder().title("认证中心swagger接口文档")
						.contact(new Contact("小威老师", "", "xiaoweijiagou@163.com")).version("1.0").build())
				.select().paths(PathSelectors.any()).build();
	}
}
```

### 2.3 ElasticSearchConfig配置
```java
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticSearchConfig {

	private String clusterName;

	private String clusterNodes;

    /**
     * 使用elasticsearch实现类时才触发
     *
     * @return
     */
	@Bean
    @ConditionalOnBean(value = EsLogServiceImpl.class)
	public TransportClient getESClient() {
		// 设置集群名字
		Settings settings = Settings.builder().put("cluster.name", this.clusterName).build();
		TransportClient client = new PreBuiltTransportClient(settings);
		try {
			// 读取的ip列表是以逗号分隔的
			for (String clusterNode : this.clusterNodes.split(",")) {
				String ip = clusterNode.split(":")[0];
				String port = clusterNode.split(":")[1];
				((TransportClient) client)
						.addTransportAddress(new TransportAddress(InetAddress.getByName(ip), Integer.parseInt(port)));
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		return client;
	}
}
```

### 2.4 RabbitmqConfig配置
```java
@Configuration
public class RabbitmqConfig {
	/**
	 * 声明队列
	 * 
	 * @return
	 */
	@Bean
	public Queue logQueue() {
		Queue queue = new Queue(LogQueue.LOG_QUEUE);

		return queue;
	}
}
```

### 2.5 线程池异步
```java
/** 线程池异步 */
@EnableAsync(proxyTargetClass = true)
@Configuration
public class AsycTaskExecutorConfig {

	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(50);
		taskExecutor.setMaxPoolSize(100);

		return taskExecutor;
	}
}

/** 资源管理器 */
@EnableResourceServer
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.csrf().disable().exceptionHandling()
				.authenticationEntryPoint(
						(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
				.and().authorizeRequests().antMatchers(PermitAllUrl.permitAllUrl("/logs-anon/**")).permitAll() // 放开权限的url
				.anyRequest().authenticated().and().httpBasic();
	}
}
```

## 3 编写业务类
### 3.1 FILTER类
```java
/**
 * 从mq队列消费日志数据
 * 
 * @author 小威老师 xiaoweijiagou@163.com
 *
 */
@Component
@RabbitListener(queues = LogQueue.LOG_QUEUE) // 监听队列
public class LogConsumer {

	private static final Logger logger = LoggerFactory.getLogger(LogConsumer.class);

	@Autowired
	private LogService logService;

	/**
	 * 处理消息
	 * 
	 * @param log
	 */
	@RabbitHandler
	public void logHandler(Log log) {
		try {
			logService.save(log);
		} catch (Exception e) {
			logger.error("保存日志失败，日志：{}，异常：{}", log, e);
		}

	}
}
```

### 3.2 SERVICES类
```java
@Service
public class LogServiceImpl implements LogService {

	@Autowired
	private LogDao logDao;

	/**
	 * 将日志保存到数据库<br>
	 * 注解@Async是开启异步执行
	 *
	 * @param log
	 */
	@Async
	@Override
	public void save(Log log) {
		if (log.getCreateTime() == null) {
			log.setCreateTime(new Date());
		}
		if (log.getFlag() == null) {
			log.setFlag(Boolean.TRUE);
		}

		logDao.save(log);
	}

	@Override
	public Page<Log> findLogs(Map<String, Object> params) {
		int total = logDao.count(params);
		List<Log> list = Collections.emptyList();
		if (total > 0) {
			PageUtil.pageParamConver(params, true);

			list = logDao.findData(params);
		}
		return new Page<>(total, list);
	}
}
```

## 3 修改配置
### 3.1 bootstrap.yml
```yml
spring:
  application:
    name: log-center
  cloud:
    config:
      discovery:
        enabled: true
        serviceId: config-center
      profile: dev
      fail-fast: true
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
```

### 3.2 log-center.yml
```yml
logging:
  level:
    root: info
    com.cloud: debug
  file: logs/${spring.application.name}.log
spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://local.mysql.com:3306/cloud_log?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false
    username: test
    password: 123456
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      connection-test-query: SELECT 1
  rabbitmq:
    host: local.rabbitmq.com
    port: 5672
    username: test
    password: test
    virtual-host: /
    listener:
      simple:
        concurrency: 20
        max-concurrency: 50
  mvc:
    servlet:
      load-on-startup: 1
  aop:
    proxy-target-class: true
  zipkin:
    base-url: http://localhost:9411
    enabled: true
    sender:
      type: web
mybatis:
  type-aliases-package: com.cloud.model.log
  mapper-locations: classpath:/mybatis-mappers/*
  configuration:
    mapUnderscoreToCamelCase: true
security:
  oauth2:
    resource:
      user-info-uri: http://local.gateway.com:8080/api-o/user-me
      prefer-token-info: false
elasticsearch:
  clusterName: elasticsearch
  clusterNodes: 127.0.0.1:9300
```