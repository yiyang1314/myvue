# Spring MVC环境
在Spring MVC环境中,首先引入swagger-bootstrap-ui的jar包文件
```xml
<dependency>
  <groupId>com.github.xiaoymin</groupId>
  <artifactId>swagger-bootstrap-ui</artifactId>
  <version>1.9.3</version>
</dependency>
```

然后,需要在Spring的XML配置文件中注入MarkdownFiles类的实例bean

如下：
```xml
<!--注入自定义文档的bean-->
<bean id="markdownFiles" class="io.swagger.models.MarkdownFiles" init-method="init">
    <property name="basePath" value="classpath:markdown/*"></property>
</bean>
```
其他例如开启增强等操作和Spring Boot环境无异,打开doc.html即可访问看到效果

#demo示例



# 自定义api文档说明
-------

### 1.版本说明
软件	版本	增强注解	说明
* knife4j	<=2.0.0	@EnableSwaggerBootstrapUi
>@Configuration
 @EnableSwagger2
 @EnableSwaggerBootstrapUi
 @Import(BeanValidatorPluginsConfiguration.class)

	
* knife4j	>=2.0.1	@EnableKnife4j	后续版本不会再更改
>@Configuration
@EnableSwagger2
@EnableKnife4j
@Import(BeanValidatorPluginsConfiguration.class)

### 2.swaggerconfig配置
```java
 @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.myboot.knife4j.contorller"))
                .paths(PathSelectors.any())
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("用户调用kinfe4jAPI接口")
                .description("服务 [yiyang]")
                .contact("易阳科技有限公司,http://www.yiyang1234.com")
                .termsOfServiceUrl("http://localhost:8081/")
                .version("1.2.0")
                .build();
        //访问地址：http://localhost:8001/websocket/doc.html
    }
```

### 3.常用注解
------
* @Api(description="API测试接口",value="person")
* @ApiOperation(value="查询用户列表 ",notes="获取所有用户信息")
* @ApiOperationSupport(author = "xiaoymin@foxmail.com") 开发者
* @ApiSupport(author = "xiaoymin@foxmail.com",order = 284） 开发者排序
  @ApiModelProperty(value="姓名",example="张飞") 

### 4.自定义文档开启
------
在markdown下新建md文件
然后配置
>swagger:
  markdowns: classpath:markdown/*