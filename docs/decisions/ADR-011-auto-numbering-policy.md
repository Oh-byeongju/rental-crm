# ADR-011 — 비즈니스 키 자동 채번 정책 (Contract / Billing / Payment)

상태: **Accepted** (2026-05-12)
영향 범위: 계약 / 청구 / 수납 — 3 도메인의 NO 컬럼 자동 채번

---

## 컨텍스트

`CT_CONTRACT.CONTRACT_NO` / `BL_BILLING.BILLING_NO` / `BL_PAYMENT.PAYMENT_NO` 는 사람이 읽는 UNIQUE 비즈니스 키. PK 는 시퀀스 ID 별도 (`{X}_ID`). 비즈니스 키 형식:

- 계약: `CT-YYYYMMDD-NNNNN` (예: `CT-20260512-00042`)
- 청구: `BILL-YYYYMM-NNNNNN` (예: `BILL-202605-000001`)
- 수납: `PAY-YYYYMM-NNNNNN` (예: `PAY-202605-000001`)

`NNNN(N)` 부분의 시퀀스 값 결정 방식이 의사결정 대상.

---

## 검토한 대안

### A. 별도 SEQUENCE + native SQL `NEXTVAL FROM DUAL`

```sql
CREATE SEQUENCE SEQ_CT_CONTRACT_NO START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
```
```java
@Query(value = "SELECT SEQ_CT_CONTRACT_NO.NEXTVAL FROM DUAL", nativeQuery = true)
Long nextContractNoSequence();
```

- **장점**: 깔끔. INSERT 전 NO 결정. 1회 INSERT.
- **단점**: 시퀀스 추가 (3개 신규). 도메인마다 별도 채번 함수 / native SQL 호출 1회 추가. 시퀀스 alloc=1 필요 (alloc=50 시 ID 점프).

### B. `fn_sy_get_seq` 같은 채번 함수 (참고 프로젝트 GDI 방식)

```sql
SELECT TO_CHAR(SYSDATE, 'YYYYMMDD') || LPAD(fn_sy_get_seq('01', ...), 8, '0')
```

- **장점**: 일자별/도메인별 시퀀스 자동 관리. 일자가 바뀌면 NNNN 1 부터.
- **단점**: PL/SQL 함수 학습 부담. rental-crm 스택에 안 맞음 (학습 시나리오 Ch.1-3 와 별개).

### C. **임시값 INSERT → contractId 받고 UPDATE** (JPA dirty checking) ✅ 채택

```java
Contract draft = Contract.builder().contractNo("TMP-" + System.nanoTime()).build();
Contract saved = contractRepository.save(draft);  // INSERT
saved.assignContractNo(generateNo(saved.getContractId()));  // 트랜잭션 끝에 UPDATE 자동
```

- **장점**: 시퀀스 추가 X. 별도 native SQL X. 같은 트랜잭션 안에서 INSERT + UPDATE.
- **단점**: INSERT + UPDATE 2회 발생. UNIQUE constraint 의 임시값 충돌 가능성 (UUID/nanoTime 으로 회피).

---

## 결정

**C 채택.** 3 도메인 (Contract / Billing / Payment) 모두 동일 패턴.

NO 형식 매핑:
- Contract: `CT-yyyyMMdd-` + contractId zero-padded 5자리
- Billing: `BILL-yyyyMM-` + billingId zero-padded 6자리
- Payment: `PAY-yyyyMM-` + paymentId zero-padded 6자리

---

## 근거

1. **학습 시나리오 정합**: Ch.1 핵심은 bulk INSERT 성능, Ch.3 핵심은 Kafka 멱등성. 자동 채번은 학습 핵심 아님 — 단순 패턴 유지.
2. **시퀀스 alloc=50** 정책 (ADR-001 §2-2) 유지. NO 용 별도 시퀀스 alloc=1 도입 시 일관성 깨짐.
3. **임시값 UNIQUE 충돌 회피**: `"TMP-" + System.nanoTime()` — 같은 트랜잭션 내 단발성. UNIQUE 충돌 확률 사실상 0.
4. **trade-off 인정**: INSERT + UPDATE 2회. 대량 등록 (Ch.1 월 청구 배치 5만건) 시 영향 측정 필요. 운영 단계에서 별도 시퀀스 도입 검토 가능.

---

## 사후 평가 (2026-05-12 — 이미 적용)

- Contract / Payment Service 에 동일 패턴 적용 ✅
- Billing 도 적용 예정 (Ch.1 배치 작성 시)
- 임시값 충돌 사례 — 아직 없음 (운영 미진입)
- 대량 등록 영향 — Ch.1 컨테이너 검증 시 측정 예정

---

## 후속 검토 사항

- 월 청구 배치 5만건 등록 시 INSERT + UPDATE 2회 = 10만 쿼리. 성능 영향 측정 후 별도 시퀀스 + bulk INSERT 패턴 전환 검토 (Ch.1 학습 핵심과 직결).
- 운영 단계: PAYMENT_NO 동시 등록 시 같은 nanoTime 충돌 가능성 (이론적 — 1조분의 1). 운영 시 UUID 로 확장 검토.
