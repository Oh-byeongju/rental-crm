# ADR-005 (Backend Local) — 감사 9컬럼 자동 주입 메커니즘

- **작성일**: 2026-05-11
- **상태**: 확정
- **선행 ADR**: ADR-001 (감사 컬럼 9개 정의 — `WRK_RMK` + `FIRS_REG_*`/`FINA_REG_*`)
- **위치**: backoffice 로컬 ADR (전역 영향 없음 — JPA 메커니즘 구현 한정)

---

## 1. Context

ADR-001 에서 모든 테이블에 9컬럼 적용 결정. 매 INSERT/UPDATE 시 9컬럼 직접 채우는 보일러플레이트를 자동화하지 않으면 코드 가독성·실수 가능성 문제.

자동 주입할 컬럼:
- 시간(2): `FIRS_REG_DTS`, `FINA_REG_DTS` — JPA Auditing 표준 (`@CreatedDate`, `@LastModifiedDate`)
- 사용자(2): `FIRS_REG_USER_ID`, `FINA_REG_USER_ID` — SecurityContext 의 인증 정보 (`@CreatedBy`, `@LastModifiedBy`)
- 프로그램(2): `FIRS_REG_PGM_ID`, `FINA_REG_PGM_ID` — HTTP 요청의 URL/엔드포인트 식별자
- IP(2): `FIRS_REG_IP`, `FINA_REG_IP` — HttpServletRequest 의 클라이언트 IP
- 비고(1): `WRK_RMK` — 사용자 입력 (자동 주입 X)

---

## 2. Decision

### 2-1. 분담 — 4 자동 + 4 반자동 + 1 수동

| 컬럼 | 메커니즘 | 출처 |
|---|---|---|
| `FIRS_REG_DTS` / `FINA_REG_DTS` | **JPA Auditing** (`@CreatedDate` / `@LastModifiedDate`) | JVM 시간 |
| `FIRS_REG_USER_ID` / `FINA_REG_USER_ID` | **JPA Auditing** (`@CreatedBy` / `@LastModifiedBy`) + `AuditorAware<String>` | `SecurityContextHolder` |
| `FIRS_REG_PGM_ID` / `FIRS_REG_IP` / `FINA_REG_PGM_ID` / `FINA_REG_IP` | **`@PrePersist`/`@PreUpdate`** + `AuditContext` (ThreadLocal) | HTTP 요청 (Filter 가 ThreadLocal 세팅) |
| `WRK_RMK` | **수동** | 사용자 입력 |

### 2-2. 구조

```
com.rental.crm.common.entity
├── BaseAuditEntity                  ← 모든 도메인 엔티티의 부모 (@MappedSuperclass)

com.rental.crm.common.audit
├── AuditContext                     ← ThreadLocal — pgm_id / ip
├── AuditContext.AuditInfo
├── AuditContextFilter               ← HTTP 요청마다 ThreadLocal set/clear
└── SpringSecurityAuditorAware       ← @CreatedBy / @LastModifiedBy 값 공급

com.rental.crm.common.config
└── JpaAuditingConfig                ← @EnableJpaAuditing 활성화
```

### 2-3. 비인증 컨텍스트 처리

배치 / 스케줄러 / Kafka Consumer 등 HTTP 요청 컨텍스트 밖에서 INSERT 시:
- `SecurityContext` 비어있음 → `AuditorAware` 가 `"SYSTEM"` 반환
- `AuditContext` 비어있음 → `AuditInfo.defaults()` (pgm_id=`"SYSTEM"`, ip=`"127.0.0.1"`) 반환
- 명시적으로 다른 값 주입하려면 배치 진입점에서 `AuditContext.set(...)` 호출 + finally 절에서 `clear()`

### 2-4. 대안 검토

| 대안 | 채택? | 사유 |
|---|---|---|
| **Hibernate Interceptor / EventListener** | ❌ | 전역 인터셉터 — 학습 시 흐름 추적 어려움. JPA 표준 미사용 |
| **순수 `@PrePersist`/`@PreUpdate` 9컬럼 모두** | ❌ | JPA Auditing 어노테이션의 표준성 포기 — 신규 개발자 학습 곡선 |
| **AOP (Aspect)** | ❌ | 너무 마법적. 단순한 데이터 주입에 AOP 과함 |
| **본 결정 — JPA Auditing + @PrePersist 혼합** | ✅ | 표준 + 명시적. 학습 가치 + 운영 안정성 |

---

## 3. 구현 세부

### BaseAuditEntity (요지)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseAuditEntity {

    @Column(name = "WRK_RMK", length = 100)
    private String wrkRmk;

    @Column(name = "FIRS_REG_PGM_ID", length = 50, nullable = false, updatable = false)
    private String firsRegPgmId;

    @CreatedDate
    @Column(name = "FIRS_REG_DTS", nullable = false, updatable = false)
    private LocalDateTime firsRegDts;

    @CreatedBy
    @Column(name = "FIRS_REG_USER_ID", length = 20, nullable = false, updatable = false)
    private String firsRegUserId;

    @Column(name = "FIRS_REG_IP", length = 50, nullable = false, updatable = false)
    private String firsRegIp;

    @Column(name = "FINA_REG_PGM_ID", length = 50, nullable = false)
    private String finaRegPgmId;

    @LastModifiedDate
    @Column(name = "FINA_REG_DTS", nullable = false)
    private LocalDateTime finaRegDts;

    @LastModifiedBy
    @Column(name = "FINA_REG_USER_ID", length = 20, nullable = false)
    private String finaRegUserId;

    @Column(name = "FINA_REG_IP", length = 50, nullable = false)
    private String finaRegIp;

    @PrePersist
    void prePersist() {
        var ctx = AuditContext.current();
        this.firsRegPgmId = ctx.pgmId();
        this.firsRegIp    = ctx.ip();
        this.finaRegPgmId = ctx.pgmId();
        this.finaRegIp    = ctx.ip();
    }

    @PreUpdate
    void preUpdate() {
        var ctx = AuditContext.current();
        this.finaRegPgmId = ctx.pgmId();
        this.finaRegIp    = ctx.ip();
    }
}
```

---

## 4. Consequences

### 긍정
- 9컬럼 채우는 코드를 모든 엔티티에서 제거 — INSERT/UPDATE 코드 가독성 ↑
- JPA Auditing 표준 사용 — 다른 Spring 개발자가 코드 읽기 쉬움
- 비인증 컨텍스트(배치) 도 `"SYSTEM"` 으로 일관 처리
- SecurityContext 와 자연스럽게 통합

### 부정 / 비용
- `BaseAuditEntity` 상속 강제 — 모든 도메인 엔티티의 부모 변경 불가 (자유도 ↓, 합리적 trade-off)
- ThreadLocal 사용 — 비동기 처리 시 (`@Async`, CompletableFuture 등) ThreadLocal 전파 안 됨 → 별도 처리 필요. 현재 학습 범위엔 없음
- JPA Auditing 활성화 = `@EnableJpaAuditing` 필요

---

## 5. 검증 방법

`Customer` 엔티티 INSERT 후 DB 에서 9컬럼 채워졌는지 확인:
```sql
SELECT WRK_RMK, FIRS_REG_PGM_ID, FIRS_REG_DTS, FIRS_REG_USER_ID, FIRS_REG_IP,
       FINA_REG_PGM_ID, FINA_REG_DTS, FINA_REG_USER_ID, FINA_REG_IP
  FROM CT_CUSTOMER WHERE CUSTOMER_ID = ?;
```

기대값:
- `FIRS_REG_*` / `FINA_REG_*` 모두 채워짐
- `WRK_RMK` 는 사용자 입력값 또는 NULL

---

## 6. 추후 검토

- 비동기 처리 시 ThreadLocal 전파 — Spring `RequestContextHolder` 또는 `TaskDecorator` 검토
- Kafka Consumer 컨텍스트에서 어떤 pgm_id 사용할지 — 토픽명을 pgm_id 로 사용 고려
