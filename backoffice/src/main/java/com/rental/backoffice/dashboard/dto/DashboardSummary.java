package com.rental.backoffice.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 대시보드 요약 위젯 — 07 API 명세 §12-1 / 04 기능명세 §9.
 *
 * <p>필드명 = 응답 JSON 키 = `dashboard.html` Thymeleaf 바인딩 키. 변경 시 3곳 동기.
 * Redis 캐시(`dashboard:summary`, TTL 5분)에 JSON 직렬화 저장.
 * {@code GenericJackson2JsonRedisSerializer} 의 내부 ObjectMapper 는 파라미터명
 * 추론(ParameterNamesModule)을 보장하지 않으므로 캐시 HIT 역직렬화 실패를 막기 위해
 * {@code @JsonCreator}/{@code @JsonProperty} 로 생성자 바인딩을 명시한다.
 */
public record DashboardSummary(
        @JsonProperty("totalActiveContracts")    long totalActiveContracts,
        @JsonProperty("thisMonthBillingCount")   long thisMonthBillingCount,
        @JsonProperty("unpaidCount")             long unpaidCount,
        @JsonProperty("unpaidAmount")            long unpaidAmount,
        @JsonProperty("thisMonthPaymentAmount")  long thisMonthPaymentAmount
) {
    @JsonCreator
    public DashboardSummary {}
}
