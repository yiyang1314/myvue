# Spring Boot单服务架构
如果你是Spring Boot的单体架构,所有的服务Controller接口都是写在一起的,那么使用knife4j的方式就很简单了,你只需要引入starter即可

maven中的pom.xml文件引入starter即可
```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-spring-boot-starter</artifactId>
    <!--在引用时请在maven中央仓库搜索最新版本号-->
    <version>2.0.2</version>
</dependency>
```
* knife4j-spring-boot-starter主要为我们引用的相关jar包：
> knife4j-spring:Swagger增强处理类
> knife4j-spring-ui:swagger的增强ui文档
> springfox-swagger:springfox最新2.9.2版本
> springfox-swagger-ui:springfox提供的ui
> springfox-bean-validators：springfxo验证支持组件

此时,位于包路径com.github.xiaoymin.knife4j.spring.configuration.Knife4jAutoConfiguration.java
类会为我们开启Swagger的增强注解,您只需要在项目中创建Swagger的Docket对象即可


#Spring Cloud微服务架构
在微服务架构下,引入微服务的starter
```xml
<dependency>
    <groupId>com.github.xiaoymin</groupId>
    <artifactId>knife4j-micro-spring-boot-starter</artifactId>
    <!--在引用时请在maven中央仓库搜索最新版本号-->
    <version>2.0.2</version>
</dependency>
```
* knife4j-micro-spring-boot-starter的区别在于去掉了Swagger的前端UI包,只引入了后端的Java代码模块
主要包含的核心模块jar：

> knife4j-spring:Swagger增强处理类
> springfox-swagger:springfox最新2.9.2版本
> springfox-bean-validators：springfxo验证支持组件

