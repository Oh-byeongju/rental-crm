package com.rental.backoffice.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * batch 모듈 호출용 RestClient (ADR-014 Step 5).
 *
 * <p>RestClient = Spring 6.1 / Boot 3.2+ 의 신규 동기 HTTP client (RestTemplate 후계).
 * fire-and-forget 은 호출 측 Controller 가 즉시 200 반환 + batch 측 {@code @Async} 로 달성.
 * client 자체는 동기 호출이라 응답까진 기다리지만 batch 가 곧장 202 ACCEPTED 를 돌려주므로 차단 시간 짧음 (~수 ms).
 */
@Configuration
public class BatchClientConfig {

    @Bean
    public RestClient batchRestClient(
            @Value("${app.internal.batch.base-url}") String baseUrl,
            @Value("${app.internal.batch.token}") String token) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Token", token)
                .build();
    }
}
