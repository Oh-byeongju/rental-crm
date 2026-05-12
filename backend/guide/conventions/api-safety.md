---
description: "Controller / Service 작성 시 — 서버측 검증 (IDOR, 한도, 상태 재조회, UNIQUE 사전 검증)"
---

# API 인터페이스 안전 규칙 (서버측 검증)

> 모든 Controller / Service 는 **클라이언트 payload 를 신뢰하지 않는다** 를 전제로 작성한다.
> 프론트(웹/모바일) 의 검증은 UX 용. 실제 정합성은 **서버에서 다시 확인** 한다.
> API 는 cURL / Postman / 스크립트로 직접 호출 가능 — 화면을 우회한 공격을 항상 가정.

---

## 1. 핵심 원칙

> **클라이언트는 "무엇을 하고 싶은지(의도)" 만 보낸다. 권한·상태·한도·존재 여부는 서버가 DB 를 다시 읽어 검증한다.**

| 분류 | 출처 | 신뢰도 | 사용 |
|---|---|---|---|
| **의도** (어떤 row 를 어떻게 바꿀지) | 클라이언트 DTO | 변조 가능 | 검증 대상 |
| **권한·상태** (수정 가능 여부) | DB SELECT (Repository) | 신뢰 가능 | 기준 |
| **한도·잔액** (예산·사용량 등) | DB SELECT (Repository) | 신뢰 가능 | 기준 |
| **존재 여부** (FK / 연관 row) | DB SELECT (Repository) | 신뢰 가능 | 기준 |

> ⚠️ **금지**: 한도/상태를 클라이언트 DTO 에서 읽고 그 값으로 검증.
> 공격자가 한도와 사용량을 둘 다 조작하면 무력화됨 (`limit=999, used=999` 같이 보내면 통과).

---

## 2. 검증 패턴

### 2-1. 상태 / 권한 재검증

상태 전이가 일어나는 모든 API 는 시작 시 DB 의 현재 상태를 다시 읽고 허용 여부를 검증한다.

```java
@Transactional
public ContractResponse suspend(Long contractId, ContractSuspendRequest req) {
    // 0. 상태 재검증 — 클라이언트의 contractStatus 는 신뢰하지 않음
    Contract contract = contractRepository.findById(contractId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "계약 없음: " + contractId));

    if (!Contract.SUSPENDABLE_STATUS.contains(contract.getContractStatus())) {
        throw new BusinessException(ErrorCode.BUSINESS_RULE,
                "현재 상태에서는 일시정지할 수 없습니다: " + contract.getContractStatus());
    }

    contract.suspend(req.suspendReason());
    return ContractResponse.from(contract);
}
```

**왜**: 사용자가 화면을 열어둔 사이 다른 사용자/배치가 상태를 바꿨을 수 있고, 공격자가 화면을 우회해서 종결 상태에 수정 요청을 보낼 수도 있음.

---

### 2-2. 한도 (잔액·금액) 검증

수량/금액 비교는 **DB 에서 한도를 읽어** 클라이언트가 보낸 사용량과 비교한다.

```java
@Transactional
public PaymentResponse pay(Long billingId, PaymentRequest req) {
    // 한도 (청구금액) 의 source of truth = DB
    Billing billing = billingRepository.findById(billingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "청구 없음"));

    long alreadyPaid = paymentRepository.sumCompletedAmountByBillingId(billingId);
    long remaining   = billing.getBillingAmount() - alreadyPaid;

    if (req.paymentAmount() > remaining) {
        throw new BusinessException(ErrorCode.BUSINESS_RULE,
                "잔여 청구금액 초과: 잔여 " + remaining + " / 요청 " + req.paymentAmount());
    }
    // ... 이후 PAYMENT INSERT
}
```

**왜**: 프론트에서 검증해도 API 직접 호출 시엔 우회됨. 한도 자체를 클라이언트가 같이 보내면 한도도 조작 가능 → 반드시 **DB 에서 한도를 다시 읽어야** 함.

---

### 2-3. UNIQUE / 충돌 사전 검증 (UX)

DB UNIQUE 제약은 충돌 시 raw 에러로 트랜잭션을 롤백시킨다 (Oracle `ORA-00001`). 사용자에게 "왜 실패했는지" 알려주려면 트랜잭션 안에서 사전 SELECT 한 번 추가.

```java
@Transactional
public EquipmentResponse register(EquipmentCreateRequest req) {
    if (equipmentRepository.existsByEquipmentCode(req.equipmentCode())) {
        throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                "이미 등록된 장비코드: " + req.equipmentCode());
    }
    if (equipmentRepository.existsByModelNameAndManufacturer(req.modelName(), req.manufacturer())) {
        throw new BusinessException(ErrorCode.ALREADY_EXISTS,
                "이미 등록된 모델: " + req.modelName() + " / " + req.manufacturer());
    }
    // ... INSERT
}
```

**왜**: UNIQUE 위반 (`ORA-00001`) 은 안전망 역할. 사용자 친화적 메시지를 위해 사전 SELECT 가 필요. **사전 SELECT 도 트랜잭션 안에서** 해야 다른 commit 된 row 가 보임 (Oracle READ COMMITTED 기준).

---

### 2-4. 존재 / 소유권 검증

수정 / 삭제 대상이 본인 데이터인지 (또는 권한 있는 데이터인지) 확인.

```java
// 고객 포털 — 본인 계약만 조회/수정 가능
Contract contract = contractRepository.findByContractIdAndCustomerId(contractId, loginCustomerId)
        .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                "계약이 존재하지 않거나 권한이 없습니다."));
```

**왜**: PK 만 받아서 UPDATE/DELETE 하면 다른 사용자 데이터를 조작당할 수 있다 (IDOR — Insecure Direct Object Reference).

> 백오피스 (관리자) 는 ADR-009 의 AUTH 키 단위 권한 검증 — `CONTRACT_VIEW` 등 — 으로 처리. 본 §2-4 의 소유권 검증은 **고객 포털** (`/api/portal/**`) 에 주로 적용.

---

## 3. 안티패턴

### ❌ 클라이언트 DTO 의 권한 / 상태 / 한도값으로 분기

```java
// 잘못된 예 — req.contractStatus() 를 그대로 신뢰
if ("ACTIVE".equals(req.contractStatus())) { ... }   // 공격자가 임의 status 보내면 통과
```

### ❌ DB 한도 없이 클라이언트 한도값과 사용값을 비교

```java
// 잘못된 예 — DTO 안에서 한도와 사용을 자가 비교
if (req.usedAmount() > req.limitAmount()) throw ...;   // 공격자가 둘 다 조작하면 무력화
```

### ❌ 프론트가 막으니 백엔드는 안 막아도 된다

프론트 validation 은 UX. **모든 비즈니스 룰은 서버에서 한 번 더 확인** 한다.

---

## 4. 변조 가능 / 변조 불가 필드 분류

요청 DTO 의 각 필드를 두 종류로 분류하고 처리:

| 종류 | 예시 | 처리 |
|---|---|---|
| **사용자 편집 가능 (의도값)** | 비고, 사용자 입력 금액·수량, 첨부파일, 비밀번호 | 그대로 INSERT/UPDATE |
| **외부 키 / 시스템 결정값** | 트랜잭션 ID, 상태값, Toss orderId/paymentKey, 청구금액 | DB SELECT 또는 무시 |

> 💡 **외부 시스템 결정값 주의**: 외부 시스템 (Toss Payments 등) 이 결정한 값 (결제금액·승인번호 등) 은 클라이언트가 표시용으로 받았더라도, INSERT 시엔 **키만 신뢰** 하고 값은 **백엔드에서 Toss API 로 재확인** 후 덮어쓰는 것이 안전.

---

## 5. 체크리스트

새 API 작성 / 리뷰 시 확인:

- [ ] 상태 변경 API 인 경우, 시작 시 **DB 에서 현재 상태를 다시 읽고** 허용 상태인지 검증했는가?
- [ ] 한도 / 잔액 비교가 있다면, **DB 에서 한도를 다시 읽었는가**? DTO 값을 그대로 쓰진 않았는가?
- [ ] UPDATE / DELETE 대상이 **요청자 본인 또는 권한 있는 데이터** 인지 확인했는가? (고객 포털은 소유권 검증, 백오피스는 AUTH 키 검증)
- [ ] UNIQUE 충돌 가능성이 있다면 사전 `existsByXxx` 로 친절한 메시지를 만들었는가?
- [ ] 외부 시스템 결정 값을 INSERT 한다면, 키만 받고 **값은 백엔드 재조회로 덮어쓰는가**?
- [ ] 트랜잭션 경계가 정확한가? (사전 SELECT 가 트랜잭션 밖에 있으면 race window 가 커짐 — `@Transactional` 안에 포함)

---

## 관련 룰

- 삭제 방어 패턴 (3단 방어선): [`delete-defense.md`](delete-defense.md)
- 백오피스 권한 모델 (AUTH 키 단위): `docs/decisions/ADR-008`, `backend/guide/decisions/ADR-009` (※ 위치 확정 시 갱신)

---

> 출처: 참고 프로젝트의 `guide/03. coding-rules/frame/backend/api-safety.md` (rental-crm 도메인에 맞춰 MyBatis Mapper / `Map<String, Object>` / `RuntimeException` / PostgreSQL SQLState → JPA Repository / DTO record / `BusinessException(ErrorCode)` / Oracle `ORA-00001` 로 변형).
