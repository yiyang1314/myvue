# 文件中心

## 1 添加pom依赖
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
			<groupId>com.aliyun.oss</groupId>
			<artifactId>aliyun-sdk-oss</artifactId>
			<version>${aliyun-sdk-oss.version}</version>
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
public class FileCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileCenterApplication.class, args);
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

### 2.3 AliyunConfig配置
```java
/**
 * 阿里云配置
 * 
 * @author 小威老师
 *
 */
@Configuration
public class AliyunConfig {

	@Value("${file.aliyun.endpoint}")
	private String endpoint;
	@Value("${file.aliyun.accessKeyId}")
	private String accessKeyId;
	@Value("${file.aliyun.accessKeySecret}")
	private String accessKeySecret;

	/**
	 * 阿里云文件存储client
	 * 
	 */
	@Bean
	public OSSClient ossClient() {
		OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
		return ossClient;
	}

}
```

### 2.4.FileServiceFactory配置
```java
@Configuration
public class FileServiceFactory {

	private Map<FileSource, FileService> map = new HashMap<>();

	@Autowired
	private FileService localFileServiceImpl;
	@Autowired
	private FileService aliyunFileServiceImpl;

	@PostConstruct
	public void init() {
		map.put(FileSource.LOCAL, localFileServiceImpl);
		map.put(FileSource.ALIYUN, aliyunFileServiceImpl);
	}

	/**
	 * 根据文件源获取具体的实现类
	 *
	 * @param fileSource
	 * @return
	 */
	public FileService getFileService(String fileSource) {
		if (StringUtils.isBlank(fileSource)) {// 默认用本地存储
			return localFileServiceImpl;
		}

		FileService fileService = map.get(FileSource.valueOf(fileSource));
		if (fileService == null) {
			throw new IllegalArgumentException("请检查FileServiceFactory类的init方法，看是否有" + fileSource + "对应的实现类");
		}

		return fileService;
	}
}
```

### 2.5 LocalFilePathConfig配置
```java
@Configuration
public class LocalFilePathConfig {

	/**
	 * 上传文件存储在本地的根路径
	 */
	@Value("${file.local.path}")
	private String localFilePath;

	/**
	 * url前缀
	 */
	@Value("${file.local.prefix}")
	public String localFilePrefix;

	@Bean
	public WebMvcConfigurer webMvcConfigurerAdapter() {
		return new WebMvcConfigurer() {

			/**
			 * 外部文件访问<br>
			 */
			@Override
			public void addResourceHandlers(ResourceHandlerRegistry registry) {
				registry.addResourceHandler(localFilePrefix + "/**")
						.addResourceLocations(ResourceUtils.FILE_URL_PREFIX + localFilePath + File.separator);
			}

		};
	}
}
```
### 2.6 全局处理异常RestControllerAdvice
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

### 2.7 全局接口开放ResourceServerConfig
```java
@EnableResourceServer
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class ResourceServerConfig extends ResourceServerConfigurerAdapter {

	/**
	 * url前缀
	 */
	@Value("${file.local.prefix}")
	public String localFilePrefix;

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http.csrf().disable().exceptionHandling()
				.authenticationEntryPoint(
						(request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
				.and().authorizeRequests()
				.antMatchers(PermitAllUrl.permitAllUrl("/files-anon/**", localFilePrefix + "/**")).permitAll() // 放开权限的url
				.anyRequest().authenticated().and().httpBasic();
	}

}
```


## 3 编写业务类
### 3.1 DAO类
```java
@Mapper
public interface FileDao {

	@Select("select * from file_info t where t.id = #{id}")
	FileInfo getById(String id);

	@Insert("insert into file_info(id, name, isImg, contentType, size, path, url, source, createTime) "
			+ "values(#{id}, #{name}, #{isImg}, #{contentType}, #{size}, #{path}, #{url}, #{source}, #{createTime})")
	int save(FileInfo fileInfo);

	@Delete("delete from file_info where id = #{id}")
	int delete(String id);

	int count(Map<String, Object> params);

	List<FileInfo> findData(Map<String, Object> params);
}
```

### 3.2 SERVICES类
```java
/**
 * 本地存储文件<br>
 * 该实现文件服务只能部署一台<br>
 * 如多台机器间能共享到一个目录，即可部署多台
 * 
 * @author 小威老师 xiaoweijiagou@163.com
 *
 */
@Service("localFileServiceImpl")
public class LocalFileServiceImpl extends AbstractFileService {

	@Autowired
	private FileDao fileDao;

	@Override
	protected FileDao getFileDao() {
		return fileDao;
	}

	@Value("${file.local.urlPrefix}")
	private String urlPrefix;
	/**
	 * 上传文件存储在本地的根路径
	 */
	@Value("${file.local.path}")
	private String localFilePath;

	@Override
	protected FileSource fileSource() {
		return FileSource.LOCAL;
	}

	@Override
	protected void uploadFile(MultipartFile file, FileInfo fileInfo) throws Exception {
		int index = fileInfo.getName().lastIndexOf(".");
		// 文件扩展名
		String fileSuffix = fileInfo.getName().substring(index);

		String suffix = "/" + LocalDate.now().toString().replace("-", "/") + "/" + fileInfo.getId() + fileSuffix;

		String path = localFilePath + suffix;
		String url = urlPrefix + suffix;
		fileInfo.setPath(path);
		fileInfo.setUrl(url);

		FileUtil.saveFile(file, path);
	}

	@Override
	protected boolean deleteFile(FileInfo fileInfo) {
		return FileUtil.deleteFile(fileInfo.getPath());
	}
}
```

### 3.3 Controller类
```java
@RestController
@RequestMapping("/files")
public class FileController {

	@Autowired
	private FileServiceFactory fileServiceFactory;

	/**
	 * 文件上传<br>
	 * 根据fileSource选择上传方式，目前仅实现了上传到本地<br>
	 * 如有需要可上传到第三方，如阿里云、七牛等
	 * 
	 * @param file
	 * @param fileSource
	 *            FileSource
	 * 
	 * @return
	 * @throws Exception
	 */
	@LogAnnotation(module = "文件上传", recordParam = false)
	@PostMapping
	public FileInfo upload(@RequestParam("file") MultipartFile file, String fileSource) throws Exception {
		FileService fileService = fileServiceFactory.getFileService(fileSource);
		return fileService.upload(file);
	}

	/**
	 * layui富文本文件自定义上传
	 * 
	 * @param file
	 * @param fileSource
	 * @return
	 * @throws Exception
	 */
	@LogAnnotation(module = "文件上传", recordParam = false)
	@PostMapping("/layui")
	public Map<String, Object> uploadLayui(@RequestParam("file") MultipartFile file, String fileSource)
			throws Exception {
		FileInfo fileInfo = upload(file, fileSource);

		Map<String, Object> map = new HashMap<>();
		map.put("code", 0);
		Map<String, Object> data = new HashMap<>();
		data.put("src", fileInfo.getUrl());
		map.put("data", data);

		return map;
	}

	/**
	 * 文件删除
	 * 
	 * @param id
	 */
	@LogAnnotation(module = "文件删除")
	@PreAuthorize("hasAuthority('file:del')")
	@DeleteMapping("/{id}")
	public void delete(@PathVariable String id) {
		FileInfo fileInfo = fileDao.getById(id);
		if (fileInfo != null) {
			FileService fileService = fileServiceFactory.getFileService(fileInfo.getSource());
			fileService.delete(fileInfo);
		}
	}

	@Autowired
	private FileDao fileDao;

	/**
	 * 文件查询
	 * 
	 * @param params
	 * @return
	 */
	@PreAuthorize("hasAuthority('file:query')")
	@GetMapping
	public Page<FileInfo> findFiles(@RequestParam Map<String, Object> params) {
		int total = fileDao.count(params);
		List<FileInfo> list = Collections.emptyList();
		if (total > 0) {
			PageUtil.pageParamConver(params, true);

			list = fileDao.findData(params);
		}
		return new Page<>(total, list);
	}
}
```

### 3.4 FileUtil类
```java
public class FileUtil {

	public static FileInfo getFileInfo(MultipartFile file) throws Exception {
		String md5 = fileMd5(file.getInputStream());

		FileInfo fileInfo = new FileInfo();
		fileInfo.setId(md5);// 将文件的md5设置为文件表的id
		fileInfo.setName(file.getOriginalFilename());
		fileInfo.setContentType(file.getContentType());
		fileInfo.setIsImg(fileInfo.getContentType().startsWith("image/"));
		fileInfo.setSize(file.getSize());
		fileInfo.setCreateTime(new Date());

		return fileInfo;
	}

	/**
	 * 文件的md5
	 * 
	 * @param inputStream
	 * @return
	 */
	public static String fileMd5(InputStream inputStream) {
		try {
			return DigestUtils.md5Hex(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 将文件保存到本地
	 *
	 * @param file
	 * @param path
	 * @return
	 */
	public static String saveFile(MultipartFile file, String path) {
		try {
			File targetFile = new File(path);
			if (targetFile.exists()) {
				return path;
			}

			if (!targetFile.getParentFile().exists()) {
				targetFile.getParentFile().mkdirs();
			}
			file.transferTo(targetFile);

			return path;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 删除本地文件
	 *
	 * @param pathname
	 * @return
	 */
	public static boolean deleteFile(String pathname) {
		File file = new File(pathname);
		if (file.exists()) {
			boolean flag = file.delete();

			if (flag) {
				File[] files = file.getParentFile().listFiles();
				if (files == null || files.length == 0) {
					file.getParentFile().delete();
				}
			}

			return flag;
		}

		return false;
	}
}
```

## 3 修改配置
### 3.1 bootstrap.yml
```yml
spring:
  application:
    name: file-center
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

### 3.2 FILE-center.yml
```yml
logging:
  level:
    root: info
    com.cloud: debug
  file: logs/${spring.application.name}.log
spring:
  datasource:
    driver-class-name: com.mysql.jdbc.Driver
    url: jdbc:mysql://local.mysql.com:3306/cloud_file?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false
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
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
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
  type-aliases-package: com.cloud.file.model
  mapper-locations: classpath:/mybatis-mappers/*
  configuration:
    mapUnderscoreToCamelCase: true
security:
  oauth2:
    resource:
      user-info-uri: http://local.gateway.com:8080/api-o/user-me
      prefer-token-info: false
file:
  local:
    path: d:/localFile
    prefix: /statics
    urlPrefix: http://api.gateway.com:8080/api-f${file.local.prefix}
  aliyun:
    endpoint: xxx
    accessKeyId: xxx
    accessKeySecret: xxx
    bucketName: xxx
    domain: https://xxx
```

