## 管理后台
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
			<artifactId>spring-boot-starter-mail</artifactId>
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
@Slf4j
@RestControllerAdvice
public class ExceptionHandlerAdvice {

	/**
	 * feignClient调用异常，将服务的异常和http状态码解析
	 * 
	 * @param exception
	 * @param response
	 * @return
	 */
	@ExceptionHandler({ FeignException.class })
	public Map<String, Object> feignException(FeignException exception, HttpServletResponse response) {
		int httpStatus = exception.status();
		if (httpStatus >= 500) {
			log.error("feignClient调用异常", exception);
		}

		Map<String, Object> data = new HashMap<>();

		String msg = exception.getMessage();

		if (!StringUtils.isEmpty(msg)) {
			int index = msg.indexOf("\n");
			if (index > 0) {
				String string = msg.substring(index);
				if (!StringUtils.isEmpty(string)) {
					JSONObject json = JSONObject.parseObject(string.trim());
					data.putAll(json.getInnerMap());
				}
			}
		}
		if (data.isEmpty()) {
			data.put("message", msg);
		}

		data.put("code", httpStatus + "");

		response.setStatus(httpStatus);

		return data;
	}

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

### 2.4 RabbitmqConfig配置
```java
@Configuration
public class RabbitmqConfig {

	/**
	 * 角色删除队列名
	 */
	public static final String ROLE_DELETE_QUEUE = "role.delete.queue";

	/**
	 * 声明队列，此队列用来接收角色删除的消息
	 * 
	 * @return
	 */
	@Bean
	public Queue roleDeleteQueue() {
		Queue queue = new Queue(ROLE_DELETE_QUEUE);

		return queue;
	}

	@Bean
	public TopicExchange userTopicExchange() {
		return new TopicExchange(UserCenterMq.MQ_EXCHANGE_USER);
	}

	/**
	 * 将角色删除队列和用户的exchange做个绑定
	 * 
	 * @return
	 */
	@Bean
	public Binding bindingRoleDelete() {
		Binding binding = BindingBuilder.bind(roleDeleteQueue()).to(userTopicExchange())
				.with(UserCenterMq.ROUTING_KEY_ROLE_DELETE);
		return binding;
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
                .and().authorizeRequests()
                .antMatchers(PermitAllUrl.permitAllUrl("/backend-anon/**", "/favicon.ico", "/css/**", "/js/**",
                        "/fonts/**", "/layui/**", "/img/**", "/pages/**", "/pages/**/*.html", "/*.html")).permitAll() // 放开权限的url
                .anyRequest().authenticated().and().httpBasic();

        http.headers().frameOptions().sameOrigin();
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
public interface MailDao {

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("insert into t_mail(userId, username, toEmail, subject, content, status, createTime, updateTime) values(#{userId}, #{username}, #{toEmail}, #{subject}, #{content}, #{status}, #{createTime}, #{updateTime})")
    int save(Mail mail);

    int update(Mail mail);

    @Select("select * from t_mail t where t.id = #{id}")
    Mail findById(Long id);

    int count(Map<String, Object> params);

    List<Mail> findData(Map<String, Object> params);
}
```

### 3.2 services类
```java
@Slf4j
@Service
public class MailServiceImpl implements MailService {

    @Autowired
    private MailDao mailDao;
    @Autowired
    private SendMailService sendMailService;

    /**
     * 保存邮件
     *
     * @param mail
     */
    @Transactional
    @Override
    public void saveMail(Mail mail) {
        if (mail.getUserId() == null || StringUtils.isBlank(mail.getUsername())) {
            AppUser appUser = AppUserUtil.getLoginAppUser();
            if (appUser != null) {
                mail.setUserId(appUser.getId());
                mail.setUsername(appUser.getUsername());
            }
        }
        if (mail.getUserId() == null) {
            mail.setUserId(0L);
            mail.setUsername("系统邮件");
        }

        if (mail.getCreateTime() == null) {
            mail.setCreateTime(new Date());
        }
        mail.setUpdateTime(mail.getCreateTime());
        mail.setStatus(MailStatus.DRAFT);

        mailDao.save(mail);
        log.info("保存邮件：{}", mail);
    }

    /**
     * 修改未发送邮件
     *
     * @param mail
     */
    @Transactional
    @Override
    public void updateMail(Mail mail) {
        Mail oldMail = mailDao.findById(mail.getId());
        if (oldMail.getStatus() == MailStatus.SUCCESS) {
            throw new IllegalArgumentException("已发送的邮件不能编辑");
        }
        mail.setUpdateTime(new Date());

        mailDao.update(mail);

        log.info("修改邮件：{}", mail);
    }

    /**
     * 异步发送邮件
     *
     * @param mail
     */
    @Override
    @Async
    public void sendMail(Mail mail) {
        boolean flag = sendMailService.sendMail(mail.getToEmail(), mail.getSubject(), mail.getContent());
        mail.setSendTime(new Date());
        mail.setStatus(flag ? MailStatus.SUCCESS : MailStatus.ERROR); // 邮件发送结果

        mailDao.update(mail);
    }

    @Override
    public Mail findById(Long id) {
        return mailDao.findById(id);
    }

    @Override
    public Page<Mail> findMails(Map<String, Object> params) {
        int total = mailDao.count(params);
        List<Mail> list = Collections.emptyList();
        if (total > 0) {
            PageUtil.pageParamConver(params, true);

            list = mailDao.findData(params);
        }
        return new Page<>(total, list);
    }
}
```

### 3.3 controller类
```java
@RestController
@RequestMapping("/mails")
public class MailController {

    @Autowired
    private MailService mailService;

    @PreAuthorize("hasAuthority('mail:query')")
    @GetMapping("/{id}")
    public Mail findById(@PathVariable Long id) {
        return mailService.findById(id);
    }

    @PreAuthorize("hasAuthority('mail:query')")
    @GetMapping
    public Page<Mail> findMails(@RequestParam Map<String, Object> params) {
        return mailService.findMails(params);
    }

    /**
     * 保存邮件
     *
     * @param mail
     * @param send 是否发送邮件
     * @return
     */
    @LogAnnotation(module = "保存邮件")
    @PreAuthorize("hasAuthority('mail:save')")
    @PostMapping
    public Mail save(@RequestBody Mail mail, Boolean send) {
        mailService.saveMail(mail);
        if (Boolean.TRUE == send) {
            mailService.sendMail(mail);
        }

        return mail;
    }

    /**
     * 修改邮件
     *
     * @param mail
     * @param send 是否发送
     * @return
     */
    @LogAnnotation(module = "修改邮件")
    @PreAuthorize("hasAuthority('mail:update')")
    @PutMapping
    public Mail update(@RequestBody Mail mail, Boolean send) {
        mailService.updateMail(mail);
        if (Boolean.TRUE == send) {
            mailService.sendMail(mail);
        }

        return mail;
    }
}
```

### 3.4 rabbitmq 消费者类
```java
@Slf4j
@Component
@RabbitListener(queues = RabbitmqConfig.ROLE_DELETE_QUEUE)
public class RoleDeleteConsumer {

	@Autowired
	private RoleMenuDao roleMenuDao;

	/**
	 * 接收到删除角色的消息<br>
	 * 删除角色和菜单关系
	 * 
	 * @param roleId
	 */
	@RabbitHandler
	public void roleDeleteHandler(Long roleId) {
		log.info("接收到删除角色的消息,roleId:{}", roleId);
		try {
			roleMenuDao.delete(roleId, null);
		} catch (Exception e) {
			log.error("角色删除消息处理异常", e);
		}
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
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://local.mysql.com:3306/cloud_backend?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false
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
  mail:
    default-encoding: UTF-8
    host: smtp.163.com
    username:
    password:
    protocol: smtp
    test-connection: false
#    properties:
#      mail.smtp.auth: true
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
  type-aliases-package: com.cloud.backend.model,com.cloud.model.mail
  mapper-locations: classpath:/mybatis-mappers/*
  configuration:
    mapUnderscoreToCamelCase: true
security:
  oauth2:
    resource:
      user-info-uri: http://local.gateway.com:8080/api-o/user-me
      prefer-token-info: false
```

## 5. 前端设计
