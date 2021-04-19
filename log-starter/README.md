# 网关系统
转发请求
默认端口号8080

## 1 添加pom依赖
```xml
	<dependencies>
		<dependency>
                <groupId>com.cloud</groupId>
                <artifactId>commons</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-amqp</artifactId>
        </dependency>
	</dependencies>
```

## 2.编写java 配置类
### 2.1 启动类注解
```java
/**
 * aop实现日志
 *
 * @author 小威老师 xiaoweijiagou@163.com
 */
@Aspect
public class LogAop {

    private static final Logger logger = LoggerFactory.getLogger(LogAop.class);

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 环绕带注解 @LogAnnotation的方法做aop
     */
    @Around(value = "@annotation(com.cloud.model.log.LogAnnotation)")
    public Object logSave(ProceedingJoinPoint joinPoint) throws Throwable {
        Log log = new Log();
        log.setCreateTime(new Date());
        LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
        if (loginAppUser != null) {
            log.setUsername(loginAppUser.getUsername());
        }

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        LogAnnotation logAnnotation = methodSignature.getMethod().getDeclaredAnnotation(LogAnnotation.class);
        log.setModule(logAnnotation.module());

        if (logAnnotation.recordParam()) { // 是否要记录方法的参数数据
            String[] paramNames = methodSignature.getParameterNames();// 参数名
            if (paramNames != null && paramNames.length > 0) {
                Object[] args = joinPoint.getArgs();// 参数值

                Map<String, Object> params = new HashMap<>();
                for (int i = 0; i < paramNames.length; i++) {
                    Object value = args[i];
                    if (value instanceof Serializable) {
                        params.put(paramNames[i], value);
                    }
                }

                try {
                    log.setParams(JSONObject.toJSONString(params)); // 以json的形式记录参数
                } catch (Exception e) {
                    logger.error("记录参数失败：{}", e.getMessage());
                }
            }
        }

        try {
            Object object = joinPoint.proceed();// 执行原方法
            log.setFlag(Boolean.TRUE);

            return object;
        } catch (Exception e) { // 方法执行失败
            log.setFlag(Boolean.FALSE);
            log.setRemark(e.getMessage()); // 备注记录失败原因
            throw e;
        } finally {
            // 异步将Log对象发送到队列
            CompletableFuture.runAsync(() -> {
                try {
                    amqpTemplate.convertAndSend(LogQueue.LOG_QUEUE, log);
                    logger.info("发送日志到队列：{}", log);
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            });

        }

    }
}
```

### 2.2 LogAutoConfiguration
```java
@Configuration
public class LogAutoConfiguration {

    /**
     * 声明队列<br>
     * 如果日志系统已启动，或者mq上已存在队列 LogQueue.LOG_QUEUE，此处不用声明此队列<br>
     * 此处声明只是为了防止日志系统启动前，并且没有队列 LogQueue.LOG_QUEUE的情况下丢失消息
     *
     * @return
     */
    @Bean
    public Queue logQueue() {
        return new Queue(LogQueue.LOG_QUEUE);
    }

    @Autowired
    private AmqpTemplate amqpTemplate;

    /**
     * 将LogMqClient声明成Bean
     * 2018.07.29添加
     */
    @Bean
    public LogMqClient logMqClient() {
        return new LogMqClient(amqpTemplate);
    }

}
```

### 2.3 LogMqClient配置
```java
/**
 * 通过mq发送日志<br>
 * 2018.07.29添加,在LogAutoConfiguration中将该类声明成Bean，用时直接@Autowired即可
 *
 * @author 小威老师 xiaoweijiagou@163.com
 */
public class LogMqClient {

    private static final Logger logger = LoggerFactory.getLogger(LogMqClient.class);

    private AmqpTemplate amqpTemplate;

    public LogMqClient(AmqpTemplate amqpTemplate) {
        this.amqpTemplate = amqpTemplate;
    }

    public void sendLogMsg(String module, String username, String params, String remark, boolean flag) {
        CompletableFuture.runAsync(() -> {
            try {
                Log log = new Log();
                log.setCreateTime(new Date());
                if (StringUtils.isNotBlank(username)) {
                    log.setUsername(username);
                } else {
                    LoginAppUser loginAppUser = AppUserUtil.getLoginAppUser();
                    if (loginAppUser != null) {
                        log.setUsername(loginAppUser.getUsername());
                    }
                }

                log.setFlag(flag);
                log.setModule(module);
                log.setParams(params);
                log.setRemark(remark);

                amqpTemplate.convertAndSend(LogQueue.LOG_QUEUE, log);
                logger.info("发送日志到队列：{}", log);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        });
    }
}
```


## 3 编写业务类
### 3.1 spring.factories
```textmate
# META-INF/spring.factories
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.cloud.log.autoconfigure.LogAutoConfiguration,\
com.cloud.log.autoconfigure.LogAop
```