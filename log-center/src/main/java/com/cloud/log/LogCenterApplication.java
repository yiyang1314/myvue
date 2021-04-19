package com.cloud.log;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 日志中心
 * 
 * @author 小威老师 xiaoweijiagou@163.com
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
public class 90io250. {

	public static void main(String[] args) {
		SpringApplication.run(LogCenterApplication.class, args);
	}

}