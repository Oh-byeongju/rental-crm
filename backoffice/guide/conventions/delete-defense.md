---
description: "DELETE / 비활성화 API 작성 시 — 절대 삭제 불가 보장 (allow-list 기반 L1+L2 방어)"
---

# 삭제 방어 — 2단 방어선 (rental-crm 채택)

> 특정 상태에서는 **누구도** 삭제 / 비활성화 못 하게 보장하는 패턴.
> 관리자 / API 직접호출 / 화면 우회 어떤 경로로도 막힌다.

---

## 적용 시점

상태 (`*_STATUS` 류 컬럼) 나 특정 조건에 따라 삭제 허용 여부가 갈리는 모든 API.
저장(UPDATE) 도 동일 패턴 적용 가능.

**rental-crm 대상 도메인 예시:**
- 계약 (`CT_CONTRACT`) — `CONTRACT_STATUS=TERMINATED` 인 계약은 변경 금지
- 청구 (`BL_BILLING`) — `BILLING_STATUS=PAID` 인 청구는 삭제·취소 금지
- 수납 (`BL_PAYMENT`) — `PAYMENT_STATUS=CANCELLED` 인 수납은 변경 금지
- 관리자 (`CM_USER`) — 마지막 활성 관리자는 비활성화 금지 (04 §1-2)

---

## 방어선 — rental-crm 은 L1 + L2 만 채택

| 층 | 책임 | rental-crm 채택 | 우회 가능성 |
|---|---|---|---|
| **L1 — 프론트(JS)** | UX 차단 (버튼 비활성화 / confirm) | ✅ 채택 | cURL / Postman 으로 우회 가능 |
| **L2 — 백엔드(Service)** | DB 상태 **재조회** 후 allow-list 검증 → 실패 시 `BusinessException` | ✅ **핵심** | DB 직접 접근 시 우회 가능 |
| **L3 — DB(Trigger)** | Oracle `BEFORE DELETE` 트리거에서 상태 검증 | ❌ **채택 안 함** | — |

### L3 트리거 채택 안 한 이유

- 학습 부담 (Oracle PL/SQL 트리거 학습 목표 아님 — 04 학습 시나리오는 JPA bulk INSERT / 쿼리 튜닝 / Kafka 멱등성)
- 마이그레이션 / 시드 / 테스트 데이터 정리 시 트리거가 같이 막아서 운영 부담
- 상수값이 자바 코드와 PL/SQL 두 곳에 공존 → 동기화 비용

→ "관리자가 DB 콘솔에서 직접 DELETE" 경로는 본 채택으로 막히지 않음. 운영 시 DB 직접 접근은 별도 통제 (DBA 권한 분리) 로 대응.

---

## L2 표준 패턴 — JPA / Spring

```java
/**
 * 계약 해지 — TERMINATED 상태는 다시 해지 불가 (이미 해지됨).
 * ACTIVE 또는 SUSPENDED 만 해지 가능.
 */
@Transactional
public void terminate(Long contractId, ContractTerminateRequest req) {
    // 1. DB 에서 현재 상태 재조회 (클라이언트 값 신뢰 금지)
    Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                    "계약이 존재하지 않거나 권한이 없습니다."));

    // 2. allow-list 검증 — 통과 못하면 fail-closed
    if (!Contract.TERMINATABLE_STATUS.contains(contract.getContractStatus())) {
        throw new BusinessException(ErrorCode.BUSINESS_RULE,
                "현재 상태에서는 해지할 수 없습니다: " + contract.getContractStatus());
    }

    contract.terminate(req.terminateReason());
}
```

상수는 도메인 엔티티에 정적 상수로 노출:

```java
public class Contract extends BaseAuditEntity {

    /** 해지 가능 상태 (allow-list) — ADR-XXX. */
    public static final Set<String> TERMINATABLE_STATUS = Set.of(
            ContractStatus.ACTIVE.name(),
            ContractStatus.SUSPENDED.name()
    );

    // ...
}
```

### 검증 포인트

- ✅ **DB 재조회** — DTO 의 상태값 신뢰 금지
- ✅ **상수 비교** — 하드코딩 리터럴 금지 (`"TERMINATED".equals(...)` X)
- ✅ **fail-closed** — 통과 못하면 즉시 `BusinessException` (분기 누락 시 자동 차단)
- ✅ **권한 검사와 분리** — 관리자도 무조건 거치는 별개 검증 (관리자 권한이 있어도 상태 허용 안 되면 차단)
- ✅ **소프트 삭제 (USE_YN='N')** vs **하드 삭제** — 도메인별 결정. rental-crm 의 마스터성 데이터는 소프트 삭제, 이력성 (수납 / 방문) 은 상태 컬럼으로 관리

---

## 안티패턴

### ❌ 클라이언트가 보낸 상태값으로 분기

```java
// 잘못된 예 — req.contractStatus() 를 그대로 신뢰
if (Contract.TERMINATABLE_STATUS.contains(req.contractStatus())) {
    contract.terminate(...);   // 공격자가 임의 status 보내면 통과
}
```

### ❌ 권한과 상태를 묶어서 분기

```java
// 잘못된 예 — SUPER_ADMIN 이면 무조건 통과
if (isSuperAdmin || Contract.TERMINATABLE_STATUS.contains(contract.getContractStatus())) { ... }
```

권한과 상태는 **AND** 가 아니라 **별개 차원**. SUPER_ADMIN 이라도 이미 TERMINATED 인 계약은 다시 해지 불가가 정책의 핵심.

### ❌ 거부 리스트(deny-list) 기반 분기 — fail-open

```java
// 잘못된 예
if ("TERMINATED".equals(contract.getContractStatus())) {
    throw new BusinessException(...);
}
contract.terminate(...);   // 새 상태가 추가되면 자동으로 허용됨 (fail-open)
```

→ 분기는 **allow-list 기준**. 새 상태가 추가되어도 명시적으로 허용 리스트에 들어가지 않는 한 차단.

---

## 체크리스트

- [ ] L1 (프론트) — 버튼 disabled / confirm 동작 확인
- [ ] L2 (백엔드) — DB 재조회 후 allow-list 검증, fail-closed 보장
- [ ] allow-list 상수가 **엔티티 또는 도메인 상수 클래스** 에 정의되어 있는가? (매직 리터럴 금지)
- [ ] 트랜잭션 안에서 재조회 + 검증 + 변경이 한 번에 수행되는가? (`@Transactional`)
- [ ] 권한과 상태 검증이 **별개** 로 수행되는가? (AND 가 아닌 직렬)

---

## 관련 룰

- API 안전성 (서버측 검증): [`api-safety.md`](api-safety.md)
- 백오피스 권한 모델: ADR-009 (사용자별 GRANT/REVOKE)

---

> 출처: 참고 프로젝트의 `guide/03. coding-rules/frame/backend/delete-defense.md` (rental-crm 도메인에 맞춰 변형: L3 트리거는 채택 안 함 명시 + MyBatis/Map 패턴 → JPA / DTO record / `BusinessException`, PostgreSQL 트리거 예시 제거).
