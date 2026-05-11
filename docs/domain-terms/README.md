# `docs/domain-terms/` — rental-crm 도메인 용어집

> 신규 컬럼·메서드·클래스 명명 시 본 사전을 진실의 원천으로 사용.
> 참고 프로젝트(GDI) 의 `domain_suffix.txt` / `domain_term.txt` / `domain_column.txt` 메커니즘 차용.

---

## 파일 구성

| 파일 | 역할 | 출처 |
|---|---|---|
| [`suffix.md`](suffix.md) | 컬럼 접미어 → 타입/길이 매핑 | GDI 표준 + rental-crm 도메인 추가 |
| [`term.md`](term.md) | 한↔영 단어 사전 (도메인 단어) | rental-crm 도메인 신규 작성 |

---

## 사용 흐름

신규 컬럼 명명 시:

```
1. 한글 단어 → 영문 약어/풀네임 (term.md 조회)
2. 의미상 접미어 결정 (suffix.md 조회 → 타입/길이 자동 결정)
3. 기존 컬럼에 동일 의미가 있으면 그대로 재사용 (재정의 금지)
```

### 예시

자연어: *"청구 금액 컬럼 필요"*

```
청구 → BILLING (term.md)
금액 → AMOUNT (suffix.md → NUMBER)
```

→ 최종: `BILLING_AMOUNT NUMBER`

---

## 갱신 정책

- 새 단어/접미어 필요 시 본 폴더의 해당 파일 수정 → 커밋
- 변경 사유는 ADR 또는 본 파일 하단 "변경 이력" 에 기록
- 캐시 자동 갱신 스크립트는 없음 — 수동 관리 (학습 프로젝트 규모 적합)

---

## 변경 이력

- 2026-05-11: 신규 작성 (ADR-001 의 D3 항목 이행)
