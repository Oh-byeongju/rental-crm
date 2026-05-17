package com.rental.backoffice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan(basePackages = "com.rental.domain")
@EnableJpaRepositories(basePackages = "com.rental.domain")
@EnableScheduling   // 대시보드 캐시 주기 워밍 (DashboardService.refresh)
public class BackofficeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackofficeApplication.class, args);
	}

}
