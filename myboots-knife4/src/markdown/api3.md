# Maven中引入Jar包
由于是springfox-swagger的增强UI包,所以基础功能依然依赖Swagger,springfox-swagger的jar包必须引入
```xml
    <dependency>
     <groupId>io.springfox</groupId>
     <artifactId>springfox-swagger2</artifactId>
     <version>2.9.2</version>
    </dependency>
```
然后引入SwaggerBootstrapUi的jar包
```xml
    <dependency>
      <groupId>com.github.xiaoymin</groupId>
      <artifactId>swagger-bootstrap-ui</artifactId>
      <version>${lastVersion}</version>
    </dependency>
```

#编写Swagger2Config配置文件
Swagger2Config配置文件如下：
```java
    @Configuration
    @EnableSwagger2
    public class SwaggerConfiguration {
    
     @Bean
     public Docket createRestApi() {
         return new Docket(DocumentationType.SWAGGER_2)
         .apiInfo(apiInfo())
         .select()
         .apis(RequestHandlerSelectors.basePackage("com.bycdao.cloud"))
         .paths(PathSelectors.any())
         .build();
     }
    
     private ApiInfo apiInfo() {
         return new ApiInfoBuilder()
         .title("swagger-bootstrap-ui RESTful APIs")
         .description("swagger-bootstrap-ui")
         .termsOfServiceUrl("http://localhost:8999/")
         .contact("developer@mail.com")
         .version("1.0")
         .build();
     }
    }
```

#访问地址
swagger-bootstrap-ui默认访问地址是：http://${host}:${port}/doc.html

#注意事项
Springfox-swagger默认提供了两个Swagger接口,需要开发者放开权限(如果使用shiro权限控制框架等)，如果使用SwaggerBootstrapUi的增强功能,还需放开增强接口地址,所以，放开的权限接口包括3个，分别是：

/swagger-resources:Swagger的分组接口
/v2/api-docs?group=groupName:Swagger的具体分组实例接口,返回该分组下所有接口相关的Swagger信息
/v2/api-docs-ext?group=groupName:该接口是SwaggerBootstrapUi提供的增强接口地址,如不使用UI增强,则可以忽略该接口
Shiro的相关配置实例如下：

```xml
    <!---other settings-->
    <property name="filterChainDefinitions">    
        <value>     
            /swagger-resources = anon
            /v2/api-docs = anon
            /v2/api-docs-ext = anon
            /doc.html = anon
            /webjars/** = anon
            
            //others....
        </value>    
    </property>
```
SpringBoot中访问doc.html报404的解决办法

实现SpringBoot的WebMvcConfigurer接口，添加相关的ResourceHandler,代码如下：
```java
    @SpringBootApplication
    @ConditionalOnClass(SpringfoxWebMvcConfiguration.class)
    public class SwaggerBootstrapUiDemoApplication  implements WebMvcConfigurer{
    
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
            registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        }
    }
```
使用SpringMvc的朋友.在web.xml中配置了DispatcherServlet,则需要追加一个url匹配规则,如下
```xml
    <servlet>
       <servlet-name>cmsMvc</servlet-name>
       <servlet-class>org.springframework.web.servlet.DispatcherServlet</servlet-class>
       <init-param>
       <param-name>contextConfigLocation</param-name>
       <param-value>classpath:config/spring.xml</param-value>
       </init-param>
       <load-on-startup>1</load-on-startup>
    </servlet>
    <!--默认配置,.htm|.do|.json等等配置-->
    <servlet-mapping>
        <servlet-name>cmsMvc</servlet-name>
        <url-pattern>*.htm</url-pattern>
    </servlet-mapping>
    <!-- 配置swagger-bootstrap-ui的url请求路径-->
    <servlet-mapping>
       <servlet-name>cmsMvc</servlet-name>
       <url-pattern>/v2/api-docs</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
       <servlet-name>cmsMvc</servlet-name>
       <url-pattern>/swagger-resources</url-pattern>
    </servlet-mapping>
    <servlet-mapping>
       <servlet-name>cmsMvc</servlet-name>
       <url-pattern>/v2/api-docs-ext</url-pattern>
    </servlet-mapping>
```