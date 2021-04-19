# 用户中心
路径classpath:/configs下
dev文件夹下是开发环境目录
对应各个微服务的bootstrap.yml里的profile: dev
默认以各个服务的{spring.application.name}.yml
暂时放在本地，有条件者可将配置文件放在git
随机端口号，支持启动多个实例

## 1.添加pom依赖
```xml
	<dependencies>
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
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-openfeign</artifactId>
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
			<groupId>com.cloud</groupId>
			<artifactId>log-starter</artifactId>
			<version>${project.version}</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
		<!--redis连接池需要此依赖-->
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-pool2</artifactId>
        </dependency>
		<dependency>
			<groupId>org.springframework.session</groupId>
			<artifactId>spring-session-data-redis</artifactId>
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
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zipkin</artifactId>
		</dependency>
		<dependency>
			<groupId>org.apache.httpcomponents</groupId>
			<artifactId>httpclient</artifactId>
		</dependency>
	</dependencies>
```

## 2.编写java 配置类
### 2.1 启动类注解
```java
/**启动类注解*/
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class UserCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserCenterApplication.class, args);
	}

}
/** feign客户端*/
@FeignClient("notification-center")
public interface SmsClient {

	@GetMapping(value = "/notification-anon/internal/phone", params = { "key", "code" })
	public String matcheCodeAndGetPhone(@RequestParam("key") String key, @RequestParam("code") String code,
			@RequestParam(value = "delete", required = false) Boolean delete,
			@RequestParam(value = "second", required = false) Integer second);
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
		return new Docket(DocumentationType.SWAGGER_2).groupName("用户中心swagger接口文档")
				.apiInfo(new ApiInfoBuilder().title("用户中心swagger接口文档")
						.contact(new Contact("小威老师", "", "xiaoweijiagou@163.com")).version("1.0").build())
				.select().paths(PathSelectors.any()).build();
	}
}
```

### 2.3 session共享
```java
@EnableRedisHttpSession
public class SessionConfig {

}
```
### 2.4.rabbitmq配置
```java
@Configuration
public class RabbitmqConfig {
	@Bean
	public TopicExchange topicExchange() {
		return new TopicExchange(UserCenterMq.MQ_EXCHANGE_USER);
	}
}

public interface UserCenterMq {

	/**
	 * 用户系统exchange名
	 */
	String MQ_EXCHANGE_USER = "user.topic.exchange";

	/**
	 * 角色删除routing key
	 */
	String ROUTING_KEY_ROLE_DELETE = "role.delete";
}
```

### 2.5 redis配置
```java
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

@Configuration
public class RestTemplateConfig {

	@Bean
	public RestTemplate restTemplate() {
		PoolingHttpClientConnectionManager pollingConnectionManager = new PoolingHttpClientConnectionManager();
		pollingConnectionManager.setMaxTotal(200);
		pollingConnectionManager.setDefaultMaxPerRoute(200);

		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		httpClientBuilder.setConnectionManager(pollingConnectionManager);
//		httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true));
		HttpClient httpClient = httpClientBuilder.build();

		HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(
				httpClient);
		// 超时时间
		clientHttpRequestFactory.setConnectTimeout(5000);
		clientHttpRequestFactory.setReadTimeout(5000);
		clientHttpRequestFactory.setConnectionRequestTimeout(5000);

		RestTemplate restTemplate = new RestTemplate();
		restTemplate.setRequestFactory(clientHttpRequestFactory);
		restTemplate.setErrorHandler(new DefaultResponseErrorHandler());

		return restTemplate;
	}
}
```
### 2.6 线程池异步配置
```java
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
```

### 2.7 全局异常处理
```java
@RestControllerAdvice
public class ExceptionHandlerAdvice {

	@ExceptionHandler({ IllegalArgumentException.class })
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> badRequestException(IllegalArgumentException exception) {
		Map<String, Object> data = new HashMap<>();
		data.put("code", HttpStatus.BAD_REQUEST.value());
		data.put("message", exception.getMessage());

		return data;
	}
}
```

### 2.8 全局接口开放
```java
@EnableResourceServer
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.csrf().disable().exceptionHandling()
				.authenticationEntryPoint(
						(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
				.and().authorizeRequests()
				.antMatchers(PermitAllUrl.permitAllUrl("/users-anon/**", "/wechat/**")).permitAll() // 放开权限的url
				.anyRequest().authenticated().and().httpBasic();
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
```
## 3 编写业务类
### 3.1 dao类
```java
@Mapper
public interface AppUserDao {

	@Options(useGeneratedKeys = true, keyProperty = "id")
	@Insert("insert into app_user(username, password, nickname, headImgUrl, phone, sex, enabled, type, createTime, updateTime) "
			+ "values(#{username}, #{password}, #{nickname}, #{headImgUrl}, #{phone}, #{sex}, #{enabled}, #{type}, #{createTime}, #{updateTime})")
	int save(AppUser appUser);

	int update(AppUser appUser);

	@Deprecated
	@Select("select * from app_user t where t.username = #{username}")
	AppUser findByUsername(String username);

	@Select("select * from app_user t where t.id = #{id}")
	AppUser findById(Long id);

	int count(Map<String, Object> params);

	List<AppUser> findData(Map<String, Object> params);

}
```
### 3.2 services类
```java
@Slf4j
@Service
public class WechatServiceImpl implements WechatService {

    @Autowired
    private WechatConfig wechatConfig;

    private static final String WECHAT_AUTHORIZE_URL = "https://open.weixin.qq.com/connect/oauth2/authorize?appid=%s&redirect_uri=%s&response_type=code&scope=snsapi_userinfo&state=%s#wechat_redirect";
    private static final String STATE_WECHAT = "state_wechat";

    @Transactional
    @Override
    public WechatUserInfo getWechatUserInfo(String app, HttpServletRequest request, String code, String state) {
        log.info("code:{}, state:{}", code, state);
        checkStateLegal(state, request);

        WechatAccess wechatAccess = getWechatAccess(app, code);
        WechatUserInfo wechatUserInfo = wechatDao.findByOpenid(wechatAccess.getOpenid());

        if (wechatUserInfo == null) {
            wechatUserInfo = saveWechatUserInfo(app, wechatAccess);
        } else {
            updateWechatUserInfo(wechatAccess, wechatUserInfo);
        }

        return wechatUserInfo;
    }
}
```

### 3.2 controller类
```java
@Slf4j
@RestController
public class UserController {

    @Autowired
    private AppUserService appUserService;
    /**
     * 管理后台给用户分配角色
     *
     * @param id      用户id
     * @param roleIds 角色ids
     */
    @LogAnnotation(module = "分配角色")
    @PreAuthorize("hasAuthority('back:user:role:set')")
    @PostMapping("/users/{id}/roles")
    public void setRoleToUser(@PathVariable Long id, @RequestBody Set<Long> roleIds) {
        appUserService.setRoleToUser(id, roleIds);
    }

    /**
     * 获取用户的角色
     *
     * @param id 用户id
     */
    @PreAuthorize("hasAnyAuthority('back:user:role:set','user:role:byuid')")
    @GetMapping("/users/{id}/roles")
    public Set<SysRole> findRolesByUserId(@PathVariable Long id) {
        return appUserService.findRolesByUserId(id);
    }

    @Autowired
    private SmsClient smsClient;

    /**
     * 绑定手机号
     *
     * @param phone
     * @param key
     * @param code
     */
    @LogAnnotation(module = "绑定手机号")
    @PostMapping(value = "/users/binding-phone")
    public void bindingPhone(String phone, String key, String code) {
        if (StringUtils.isBlank(phone)) {
            throw new IllegalArgumentException("手机号不能为空");
        }

        if (StringUtils.isBlank(key)) {
            throw new IllegalArgumentException("key不能为空");
        }

        if (StringUtils.isBlank(code)) {
            throw new IllegalArgumentException("code不能为空");
        }

        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        log.info("绑定手机号，key:{},code:{},username:{}", key, code, loginAppUser.getUsername());

        String value = smsClient.matcheCodeAndGetPhone(key, code, false, 30);
        if (value == null) {
            throw new IllegalArgumentException("验证码错误");
        }

        if (phone.equals(value)) {
            appUserService.bindingPhone(loginAppUser.getId(), phone);
        } else {
            throw new IllegalArgumentException("手机号不一致");
        }
    }
}
```


## 3 修改配置
### 3.1 bootstrap.yml
```yml
spring:
  application:
    name: user-center
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

### 3.2 user-center.yml
```yml
logging:
  level:
    root: info
    com.cloud: debug
  file: logs/${spring.application.name}.log
spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://local.mysql.com:3306/cloud_user?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false
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
  redis:
    host: local.redis.com
    port: 6379
    password:
    timeout: 10s
    lettuce:
      pool:
        min-idle: 0
        max-idle: 8
        max-active: 8
        max-wait: -1ms
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
  type-aliases-package: com.cloud.model.user
  mapper-locations: classpath:/mybatis-mappers/*
  configuration:
    mapUnderscoreToCamelCase: true
security:
  oauth2:
    resource:
      user-info-uri: http://local.gateway.com:8080/api-o/user-me
      prefer-token-info: false
wechat:
  domain: http://api.gateway.com:8080/api-u
  infos:
    app1:
      appid: xxx
      secret: xxx
    app2:
      appid: xxx
      secret: xxx
ribbon:
  ReadTimeout: 10000
  ConnectTimeout: 10000
hystrix:
  command:
    default:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: 60000
```