# `docs/global-rules/` — 전역 작업 시점 룰 (인덱스 / 라우터)

> Claude 가 자동 로드하지 않는 **여러 디렉토리에 영향을 미치는** 작업 시점 룰.
> 작업 시작 시 본 인덱스를 보고 각 룰의 `description` frontmatter 를 참고하여 매칭되는 룰만 Read.

---

## 어떻게 동작하나

1. 사용자가 작업 요청 (예: "API 응답 포맷 정해줘", "Kafka 토픽 추가하자")
2. Claude 가 **본 README 인덱스 표** 를 본다
3. 작업과 매칭되는 룰을 description 으로 판별
4. 매칭된 룰들만 Read 하여 작업 진행

---

## 룰 인덱스

| 룰 | description | 파일 |
|---|---|---|
| DB 컨벤션 | 신규 테이블 / 컬럼 / 시퀀스 / 인덱스 설계 시 — DB 메타 룰 (타입·명명·기본값). `*_YN` VARCHAR(1) | [db-conventions.md](db-conventions.md) |
| DB 캐시 사용 패턴 | DB 스키마 / 시드 조회 시 — `docs/cache/` 텍스트 캐시 우선 (DB 직접 조회 금지) | [db-cache-pattern.md](db-cache-pattern.md) |
| 프론트엔드 그리드 라이브러리 | 신규 화면에 그리드(데이터 테이블) 사용 시 — 채택 가능 라이브러리 + 라이선스 제약 (Webix 금지) | [frontend-grid-library.md](frontend-grid-library.md) |

---

## 후보 (작업 시점에 추가될 가능성)

- API 응답 공통 포맷 (`{ code, message, data }`) — backoffice 발행 / portal 소비 → 둘 다 영향
- Kafka 토픽 명명 / 페이로드 스키마 — 발행자(backoffice) / 소비자(backoffice) 양쪽 영향
- ERD 변경 절차 — docs 산출물 / backoffice 엔티티 둘 다 영향
- 에러 코드 체계 — backoffice / portal 둘 다 사용

> 위 후보는 처음부터 만들지 않는다 — **두 디렉토리 이상에서 실제 필요해질 때** 추출 (점진적 추상화).

---

## 새 룰 추가 시

1. 본 README 인덱스 표에 행 추가
2. 룰 파일 첫 줄에 `description` frontmatter:
   ```yaml
   ---
   description: "<언제 보는지 한 줄>"
   ---
   ```
3. 적용 범위가 한 디렉토리에만 국한되면 → **로컬 룰**(`{디렉토리}/guide/`) 로 이동

상세 절차: [`@.claude/rules/_meta/rule-management.md`](../../.claude/rules/_meta/rule-management.md)
