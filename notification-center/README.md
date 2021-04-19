# 通知中心
* 1.pom依赖
* 2.config配置
* 3.dao类
* 4.services类
* 5.controller类
* 6.监听类
* 7.yml配置

## 1.pom依赖
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
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-redis</artifactId>
		</dependency>
		<!--redis连接池需要此依赖-->
		<dependency>
			<groupId>org.apache.commons</groupId>
			<artifactId>commons-pool2</artifactId>
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
			<groupId>com.aliyun</groupId>
			<artifactId>aliyun-java-sdk-core</artifactId>
			<version>${aliyun-sdk-core.version}</version>
		</dependency>
		<dependency>
			<groupId>com.aliyun</groupId>
			<artifactId>aliyun-java-sdk-dysmsapi</artifactId>
			<version>${aliyun-sdk-dysmsapi.version}</version>
		</dependency>
		<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-zipkin</artifactId>
		</dependency>
	</dependencies>
```


## 2.config配置
### 2.1 线程池配置
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
### 2.2 全局处理异常
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

### 2.3 FeignInterceptorConfig拦截
```java
/**
 * 使用feign client访问别的微服务时，将access_token放入参数或者header<br>
 * 任选其一即可，<br>
 * 如token为xxx<br>
 * 参数形式就是access_token=xxx<br>
 * header的话，是Authorization:Bearer xxx<br>
 * 我们默认放在header里
 * 
 * @author 小威老师
 *
 */
@Configuration
public class FeignInterceptorConfig {

	@Bean
	public RequestInterceptor requestInterceptor() {
		RequestInterceptor requestInterceptor = new RequestInterceptor() {

			@Override
			public void apply(RequestTemplate template) {
				Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
				if (authentication != null) {
					if (authentication instanceof OAuth2Authentication) {
						OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication.getDetails();
						String access_token = details.getTokenValue();

						template.header("Authorization", OAuth2AccessToken.BEARER_TYPE + " " + access_token);
//						template.query(OAuth2AccessToken.ACCESS_TOKEN, access_token);
					}

				}
			}
		};

		return requestInterceptor;
	}
}
```

### 2.4 阿里云短信配置
```java
/**
 * 阿里云短信配置
 */
@Configuration
public class AliyunSmsConfig {

	@Value("${aliyun.accessKeyId}")
	private String accessKeyId;
	@Value("${aliyun.accessKeySecret}")
	private String accessKeySecret;

	@Bean
	public IAcsClient iAcsClient() throws ClientException {
		// 设置超时时间-可自行调整
		System.setProperty("sun.net.client.defaultConnectTimeout", "10000");
		System.setProperty("sun.net.client.defaultReadTimeout", "10000");
		// 初始化ascClient需要的几个参数
		final String product = "Dysmsapi";// 短信API产品名称（短信产品名固定，无需修改）
		final String domain = "dysmsapi.aliyuncs.com";// 短信API产品域名（接口地址固定，无需修改）
		// 初始化ascClient,暂时不支持多region（请勿修改）
		IClientProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
		DefaultProfile.addEndpoint("cn-hangzhou", "cn-hangzhou", product, domain);

		IAcsClient acsClient = new DefaultAcsClient(profile);

		return acsClient;
	}

}
```

### 2.5 资源服务配置
```java
/**
 * 资源服务配置
 *
 * @author 小威老师
 */
@EnableResourceServer
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.csrf().disable().exceptionHandling()
				.authenticationEntryPoint(
						(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
				.and().authorizeRequests().antMatchers(PermitAllUrl.permitAllUrl("/notification-anon/**")).permitAll() // 放开权限的url
				.anyRequest().authenticated().and().httpBasic();
	}
}
/**api在线文档*/
@Configuration
@EnableSwagger2
public class SwaggerConfig {

	@Bean
	public Docket docket() {
		return new Docket(DocumentationType.SWAGGER_2).groupName("管理后台swagger接口文档")
				.apiInfo(new ApiInfoBuilder().title("管理后台swagger接口文档")
						.contact(new Contact("小威老师", "", "xiaoweijiagou@163.com")).version("1.0").build())
				.select().paths(PathSelectors.any()).build();
	}
}
```

## 3. 业务实现类
### 3.1 dao类
```java
@Mapper
public interface SmsDao {

	@Options(useGeneratedKeys = true, keyProperty = "id")
	@Insert("insert into t_sms(phone, signName, templateCode, params, day, createTime, updateTime) "
			+ "values(#{phone}, #{signName}, #{templateCode}, #{params}, #{day}, #{createTime}, #{updateTime})")
	int save(Sms sms);

	@Select("select * from t_sms t where t.id = #{id}")
	Sms findById(Long id);

	int update(Sms sms);

	int count(Map<String, Object> params);

	List<Sms> findData(Map<String, Object> params);
}
```

### 3.2 services类
```java
@Slf4j
@Service
public class SmsServiceImpl implements SmsService {

	@Autowired
	private IAcsClient acsClient;
	@Value("${aliyun.sign.name1}")
	private String signName;
	@Value("${aliyun.template.code1}")
	private String templateCode;

	@Autowired
	private SmsDao smsDao;

	/**
	 * 异步发送阿里云短信
	 *
	 * @param sms
	 * @return
	 */
	@Async
	@Override
	public SendSmsResponse sendSmsMsg(Sms sms) {
		if (sms.getSignName() == null) {
			sms.setSignName(this.signName);
		}

		if (sms.getTemplateCode() == null) {
			sms.setTemplateCode(this.templateCode);
		}

		// 阿里云短信官网demo代码
		SendSmsRequest request = new SendSmsRequest();
		request.setMethod(MethodType.POST);
		request.setPhoneNumbers(sms.getPhone());
		request.setSignName(sms.getSignName());
		request.setTemplateCode(sms.getTemplateCode());
		request.setTemplateParam(sms.getParams());
		request.setOutId(sms.getId().toString());

		SendSmsResponse response = null;
		try {
			response = acsClient.getAcsResponse(request);
			if (response != null) {
				log.info("发送短信结果：code:{}，message:{}，requestId:{}，bizId:{}", response.getCode(), response.getMessage(),
						response.getRequestId(), response.getBizId());

				sms.setCode(response.getCode());
				sms.setMessage(response.getMessage());
				sms.setBizId(response.getBizId());
			}
		} catch (ClientException e) {
			e.printStackTrace();
		}

		update(sms);

		return response;
	}

	/**
	 * 保存短信记录
	 *
	 * @param sms
	 * @param params
	 */
	@Transactional
	@Override
	public void save(Sms sms, Map<String, String> params) {
		if (!CollectionUtils.isEmpty(params)) {
			sms.setParams(JSONObject.toJSONString(params));
		}

		sms.setCreateTime(new Date());
		sms.setUpdateTime(sms.getCreateTime());
		sms.setDay(sms.getCreateTime());

		smsDao.save(sms);
	}

	@Transactional
	@Override
	public void update(Sms sms) {
		sms.setUpdateTime(new Date());
		smsDao.update(sms);
	}

	@Override
	public Sms findById(Long id) {
		return smsDao.findById(id);
	}

	@Override
	public Page<Sms> findSms(Map<String, Object> params) {
		int total = smsDao.count(params);
		List<Sms> list = Collections.emptyList();
		if (total > 0) {
			PageUtil.pageParamConver(params, true);

			list = smsDao.findData(params);
		}
		return new Page<>(total, list);
	}

}
```

### 3.4 Util类
```java
public class Util {

	private static String[] NUMBERS = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
	private static Random RANDOM = new Random();

	/**
	 * 生成n位随机数值字符串
	 * 
	 * @param n
	 * @return
	 */
	public static String randomCode(int n) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < n; i++) {
			builder.append(NUMBERS[RANDOM.nextInt(NUMBERS.length)]);
		}

		return builder.toString();
	}
}
```


## 4  bachend.yml配置
```yml
logging:
  level:
    root: info
    com.cloud: debug
  file: logs/${spring.application.name}.log
spring:
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
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://local.mysql.com:3306/cloud_notification?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false
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
  type-aliases-package: com.cloud.notification.model
  mapper-locations: classpath:/mybatis-mappers/*
  configuration:
    mapUnderscoreToCamelCase: true
security:
  oauth2:
    resource:
      user-info-uri: http://local.gateway.com:8080/api-o/user-me
      prefer-token-info: false
aliyun:
  accessKeyId: xxx
  accessKeySecret: xxx
  sign:
    name1: xxx
  template:
    code1: xxx
sms:
  expire-minute: 15
  day-count: 30
```

## 5. 前端设计
