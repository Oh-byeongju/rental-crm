# ADR-012 — 상태 전이 allow-list 패턴

상태: **Accepted** (2026-05-12)
영향 범위: Contract / Visit / Billing / Payment / Overdue + Code (시스템 그룹 차단) — 6 도메인

---

## 컨텍스트

상태(`*_STATUS` 류 컬럼)를 가진 도메인에서 상태 전이 검증 시 어떤 패턴을 채택할지. ref-project 의 `delete-defense.md` 룰 도입 시점 (2026-05-12) 에 패턴 통일 결정.

대상 도메인:
- 계약 (`CT_CONTRACT.CONTRACT_STATUS`): ACTIVE / SUSPENDED / TERMINATED
- 방문 (`CT_VISIT.VISIT_STATUS`): SCHEDULED / COMPLETED / CANCELLED
- 청구 (`BL_BILLING.BILLING_STATUS`): UNPAID / OVERDUE / PAID / CANCELLED
- 수납 (`BL_PAYMENT.PAYMENT_STATUS`): COMPLETED / CANCELLED / REFUNDED
- 코드 (`CM_CODE_GROUP.SYSTEM_YN`): 시스템 그룹 변경 차단

---

## 검토한 대안

### A. **allow-list (허용 상태만 명시)** ✅ 채택

```java
public static final Set<String> SUSPENDABLE_STATUS = Set.of(STATUS_ACTIVE);
// Service
if (!Contract.SUSPENDABLE_STATUS.contains(contract.getContractStatus())) {
    throw new BusinessException(ErrorCode.BUSINESS_RULE, "일시정지 불가 상태: " + ...);
}
```

- **fail-closed**: 명시되지 않은 상태 자동 거부.
- 새 상태 추가 시 → allow-list 자동 차단 → 명시적으로 허용해야 통과.

### B. deny-list (거부 상태만 명시)

```java
if (contract.getContractStatus().equals(STATUS_TERMINATED)) {
    throw ...;
}
```

- **fail-open**: 명시되지 않은 상태 자동 허용.
- 새 상태 추가 시 → 자동 허용 → 의도치 않은 통과 위험.

### C. 매트릭스 (from-to 매핑)

```java
public static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
    STATUS_ACTIVE,    Set.of(STATUS_SUSPENDED, STATUS_TERMINATED),
    STATUS_SUSPENDED, Set.of(STATUS_ACTIVE, STATUS_TERMINATED)
);
```

- **장점**: 전이 매트릭스 명확.
- **단점**: 보일러플레이트. 단순 도메인 (방문 — SCHEDULED→COMPLETED 만) 에는 과함.

---

## 결정

**A 채택.** 각 전이별 `{ACTION}ABLE_STATUS` 상수를 Entity 안에 정의. Service 가 `contains()` 검증 후 Entity 의 도메인 행위 메서드 호출.

명명 규칙:
- `SUSPENDABLE_STATUS` — suspend 가능 상태 set
- `RESUMABLE_STATUS` — resume 가능
- `TERMINATABLE_STATUS` — terminate 가능
- `COMPLETABLE_STATUS` / `CANCELLABLE_STATUS` / `PAYABLE_STATUS` / `REFUNDABLE_STATUS` / `OVERDUE_CANDIDATE`

---

## 근거

1. **`delete-defense.md` 룰 명시**: "분기는 allow-list 기준. 새 상태가 추가되어도 명시적으로 허용 리스트에 들어가지 않는 한 차단." (ADR-011 채택 룰의 핵심 원칙)
2. **단순함**: deny-list 안티패턴 위험 회피. 매트릭스 패턴은 단순 도메인엔 과함.
3. **위치 일관성**: Entity 안 상수로 명시 — 도메인 invariant 가 Entity 에 표현됨 (도메인 주도 설계).
4. **확장성**: 새 상태 추가 시 자동 차단 — 의도치 않은 허용 방지.
5. **Service 와 Entity 책임 분리**: Entity 는 상태 변경 (단순 setter + 시간 기록), Service 는 검증. 검증 실패 시 BusinessException (spring 의존성 Entity 격리).

---

## 사후 평가 (2026-05-12 — 6 도메인 적용)

| 도메인 | allow-list 상수 |
|---|---|
| Contract | SUSPENDABLE / RESUMABLE / TERMINATABLE_STATUS |
| Visit | COMPLETABLE / CANCELLABLE_STATUS |
| Billing | PAYABLE / CANCELLABLE / OVERDUE_CANDIDATE |
| Payment | CANCELLABLE / REFUNDABLE_STATUS |
| Code | `isSystem()` 단일 분기 (시스템 그룹 = 변경 차단) |

→ 6 도메인 모두 동일 패턴 — 룰 일관성 확보. 새 도메인 추가 시 같은 패턴 자동 채택.

---

## 후속 검토 사항

- 상태 전이 매트릭스가 복잡해지면 (예: ACTIVE→PAUSED→CANCELED→REACTIVATED 5 단계+) 매트릭스 패턴 (C) 으로 전환 검토.
- 현재까지는 단순 (각 도메인 3-4 상태) — allow-list 충분.
- L3 (DB 트리거) 채택 안 함 명시 (`delete-defense.md`). 운영 시 DB 콘솔 직접 변경 막으려면 DBA 권한 분리로 대응.
