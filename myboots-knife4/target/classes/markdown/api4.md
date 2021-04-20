# 3.2 Spring Boot自动注册
如果你的项目是通过Spring Boot进行开发,并且不想通过Knife4jCloud提供的界面进行操作,并且已经集成了springfox-swagger组件,那么,你可以引用Knife4jCloud提供的自动注册的jar包组件进行自动注册

### 1.Maven引用
```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-discovery-spring-boot-starter</artifactId>
    <!--在引用时请在maven中央仓库(http://search.maven.org)搜索最新版本号-->
    <!-- 该版本必须和Knife4jCloud主版本一致-->
    <version>1.0</version>
</dependency>
```

### 2、在application.yml或者application.properties配置文件中配置相关参数,以yml为例：
```yml
knife4j:
  cloud:
    ## 参考注册API中的accessKey
    accessKey: JDUkd1YvSi5zZmUkMHYuSGNmN1hMazJPajJuMjNJVW43dWNyL2tyR3N4bzJaa1A2ZC5mSUlwNA
    ## 项目编号
    code: APITest
    ## Knife4jCloud的对外域名地址
    server: http://127.0.0.1:19011
    ## 当前服务是否是HTTPS的,默认可以不配置,并且该参数默认为false
    ssl: false
    ## 参考注册API中的client属性,该参数可以不配置,只有在域名的情况下需要进行配置
    client: http://test.domain.com
```

    
###3、在Spring Boot应用中通过注解@EnableKnife4jCloudDiscovery进行启用

>@EnableKnife4jCloudDiscovery
>@SpringBootApplication
>public class Knife4jSpringBootDemoApplication implements WebMvcConfigurer{
>    //more..
>}