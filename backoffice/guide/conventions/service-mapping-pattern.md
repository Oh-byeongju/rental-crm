---
description: "Service 가 페이지 응답 매핑 시 — N+1 회피 (페이지 단위 일괄 fetch + Map 변환)"
---

# Service 응답 매핑 패턴 — 페이지 단위 일괄 매핑

> Service.search() 가 페이지 응답을 만들 때 다른 도메인 정보를 함께 표시해야 하면
> 페이지마다 `findById` 반복 (N+1) 이 아니라 **한 번에 fetch + Map 변환** 한다.

---

## 적용 시점

- 페이지 응답 (`Page<Response>`) 에 다른 도메인 정보 (코드 한글명 / FK 참조 도메인 필드 등) 가 포함되는 모든 Service
- Equipment / Product / Contract / Visit / Billing Service 가 이 패턴 사용 (5+ 도메인 검증됨)

---

## 표준 패턴

```java
public Page<ProductResponse> search(ProductSearchRequest req, Pageable pageable) {
    Page<Product> page = productRepository.search(...);

    // 1. 페이지의 FK ID 모음
    Set<Long> equipmentIds = page.getContent().stream()
            .map(Product::getEquipmentId)
            .collect(Collectors.toSet());

    // 2. 일괄 fetch — findAllById (1 쿼리)
    Map<Long, Equipment> equipmentMap = equipmentRepository.findAllById(equipmentIds).stream()
            .collect(Collectors.toMap(Equipment::getEquipmentId, e -> e));

    // 3. Map 으로 매핑
    return page.map(p -> ProductResponse.from(p, equipmentMap.get(p.getEquipmentId())));
}
```

### 다중 FK — 2 단계 fetch

Contract 처럼 customer / product / equipment 3 도메인 매핑 시:

```java
Set<Long> customerIds = page.getContent().stream().map(Contract::getCustomerId).collect(Collectors.toSet());
Set<Long> productIds  = page.getContent().stream().map(Contract::getProductId).collect(Collectors.toSet());

Map<Long, Customer> customerMap = customerRepository.findAllById(customerIds).stream()
        .collect(Collectors.toMap(Customer::getCustomerId, c -> c));
Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
        .collect(Collectors.toMap(Product::getProductId, p -> p));

// product 의 FK 활용 → equipment 2단계 fetch
Set<Long> equipmentIds = productMap.values().stream()
        .map(Product::getEquipmentId).collect(Collectors.toSet());
Map<Long, Equipment> equipmentMap = equipmentRepository.findAllById(equipmentIds).stream()
        .collect(Collectors.toMap(Equipment::getEquipmentId, e -> e));

return page.map(c -> {
    Product product   = productMap.get(c.getProductId());
    Equipment equipment = product == null ? null : equipmentMap.get(product.getEquipmentId());
    return ContractResponse.from(c, customerMap.get(c.getCustomerId()), product, equipment);
});
```

### 페이지당 추가 쿼리 수

| FK 도메인 수 | 추가 쿼리 |
|---|---|
| 0 (자체 정보만) | 0 |
| 1 | 1 |
| 2 (병렬 FK) | 2 |
| 3 (2단계 FK — Product → Equipment) | 3 |

→ 페이지 크기 (20) 와 무관하게 상수 횟수. N+1 회피.

---

## 단건 매핑 — 별도 헬퍼

페이지 매핑과 별개로 단건 (`findById`, `register`, `update`, 상태 전이) 시 같은 매핑 로직 — `toResponse(entity)` private 메서드로 추출.

```java
public ContractResponse findById(Long contractId) {
    return toResponse(loadContract(contractId));
}

@Transactional
public ContractResponse suspend(Long contractId, ContractActionRequest req) {
    Contract contract = loadContract(contractId);
    // ... validate + suspend ...
    return toResponse(contract);
}

// 단건 매핑 헬퍼 — find 3회. 단건이라 N+1 무관.
private ContractResponse toResponse(Contract contract) {
    Customer customer  = customerRepository.findById(contract.getCustomerId()).orElse(null);
    Product  product   = productRepository.findById(contract.getProductId()).orElse(null);
    Equipment equipment = (product == null) ? null
            : equipmentRepository.findById(product.getEquipmentId()).orElse(null);
    return ContractResponse.from(contract, customer, product, equipment);
}
```

→ **페이지 매핑** 과 **단건 매핑** 분리. 페이지 매핑은 일괄 fetch, 단건은 단순 findById.

---

## 안티패턴

### ❌ 페이지마다 findById 반복 (N+1)

```java
// 잘못된 예 — 페이지 크기 만큼 추가 쿼리
return page.map(p -> {
    Equipment equipment = equipmentRepository.findById(p.getEquipmentId()).orElse(null);
    return ProductResponse.from(p, equipment);
});
```

페이지 크기 20 + FK 1개 → 20 추가 쿼리. 페이지 크기 100 → 100 추가 쿼리.

### ❌ JPA fetch join — 학습 단계 회피

```java
// 잘못된 예 — 학습 단계에서 fetch join 도입 부담
@Query("select p from Product p left join fetch p.equipment")
```

fetch join 은 N+1 해결하지만:
- 연관관계 매핑 (`@ManyToOne`) 필요 — 단순 `Long` FK 패턴과 충돌
- 페이징 + fetch join 함께 시 Hibernate 경고 (`HHH000104`) — `firstResult` 무시
- 학습 부담 + Cartesian product 가능성

→ rental-crm 은 **연관관계 매핑 X + 단순 Long FK + 일괄 fetch Map 변환** 패턴 채택. 명시적 + 단순 + 안전.

---

## 체크리스트

새 Service 의 search 메서드 작성 시:

- [ ] 응답에 다른 도메인 정보가 포함되는가?
- [ ] 페이지마다 `findById` 호출하는 코드가 있는가? (N+1 — 안티패턴)
- [ ] FK ID 모은 후 `findAllById` 로 한 번에 fetch 했는가?
- [ ] `Collectors.toMap` 으로 Map 변환했는가?
- [ ] 단건 매핑 (`findById`, `register`, `update`, 상태 전이) 은 별도 `toResponse(entity)` 헬퍼로 추출했는가?

---

## 적용된 도메인 (검증된 패턴)

| 도메인 | 페이지 fetch | 단건 헬퍼 |
|---|---|---|
| Equipment | typeName Map (CM_CODE) | `toResponse(e)` |
| Product | Equipment Map | `from(p, equipment)` 단건은 자체 |
| Contract | Customer + Product + Equipment Map (2단계) | `toResponse(contract)` |
| Visit | Contract + Engineer Map | `toResponse(visit)` |
| Billing | Contract + Customer Map | `toResponse(billing)` (Ch.1 작업 시) |

---

## 관련 룰

- [api-safety.md](api-safety.md) — 서버측 검증
- [delete-defense.md](delete-defense.md) — 상태 전이 allow-list
- [@.claude/rules/frame/db/sql-query.md](../../../.claude/rules/frame/db/sql-query.md) §4 — N+1 회피 일반 룰

---

> 출처: 작업 중 5+ 도메인 (Equipment / Product / Contract / Visit / Billing) 에서 동일 패턴이 재현되어 룰화. 2026-05-12 도입.
