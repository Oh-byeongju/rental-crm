# ADR-010 — 권한 캐시·invalidation·운영 정책

- **작성일**: 2026-05-11
- **상태**: 확정
- **연관**:
  - `docs/decisions/ADR-008-permission-model-auth-code.md` (AUTH 키 단위)
  - `docs/decisions/ADR-009-user-direct-auth-grant-revoke.md` (사용자 GRANT/REVOKE)
  - `backoffice/src/main/resources/application.yml` (Redis 설정)
  - 5단계 (관리자 도메인 구현) — `PermissionService` / `PermissionCacheService`

---

## 1. Context

ADR-008 (AUTH 키 행 단위) + ADR-009 (사용자 직접 권한 GRANT/REVOKE) 로 권한 *모델* 은 확정. 운영 단계 디테일이 남음:

1. 매 요청 권한 판정 비용 (UNION ∪ GRANT − REVOKE) 어떻게 줄일 것인가
2. 권한 변경 시 즉시 반영 (회수 SLA)
3. 사용자 강제 로그아웃 (해고·탈취 의심)
4. 화면 동기화 (라이브 vs 자연 새로고침)
5. AUTH_TYPE / CM_USER_AUTH 운영 디테일
6. JWT 도입 시점 재확인

본 ADR 에서 일괄 결정.

---

## 2. Decision

### 2-1. Redis 권한 캐시

| 항목 | 결정 |
|---|---|
| 키 구조 | `permissions:user:{userId}` 단일 키 |
| Value | 최종 권한 셋 JSON 배열 (역할 ∪ GRANT − REVOKE 결과) |
| TTL | 30분 (Access Token TTL 과 동기화) |
| MISS 시 | DB UNION/MINUS 쿼리 → 캐시 빌드 → 반환 |

분리 키 (`permissions:role:{roleId}` + `permissions:user:{userId}:override`) 는 비채택 — 매 요청 GET 2회 + 학습 단순화 우선.

### 2-2. Invalidation 트리거

| 변경 작업 | 무효화 대상 |
|---|---|
| `CM_USER_AUTH` INSERT/DELETE (사용자 GRANT/REVOKE 등록·해제) | `permissions:user:{userId}` |
| `CM_ROLE_AUTH` 변경 (역할에 AUTH 추가·제거) | 해당 역할 보유 모든 사용자 키 (`SCAN` + `DEL`) |
| `CM_USER.ROLE_ID` UPDATE (사용자 역할 변경) | `permissions:user:{userId}` |
| `CM_AUTH` DELETE (권한 키 마스터 삭제) | `permissions:user:*` 전체 무효화 |
| `CM_ROLE` 삭제 | 위와 동일 |

구현: `PermissionCacheService` 가 위 트리거 메서드 제공 (5단계).

### 2-3. 권한 회수 SLA

| 항목 | 보장 |
|---|---|
| 권한 변경 후 새 API 요청 | ✅ 즉시 (다음 클릭) |
| 이미 진행 중인 요청 | ❌ 그 요청은 이전 권한으로 처리 (컨트롤러 진입 시점에 권한 체크 완료) |
| 화면의 버튼 숨김 | ❌ 다음 페이지 GET 시 반영 (Thymeleaf 서버 렌더링 + 자연 새로고침) |
| Refresh Token 으로 새 Access Token 발급 시 | ✅ 캐시 재빌드 트리거 |

### 2-4. 강제 로그아웃 정책 (참고용)

**현 사이클에서는 구현하지 않음**. JWT 도입 사이클에서 결정/구현.

권장 패턴 (참고):
- Access Token 의 JTI (JWT ID) 를 Redis 블랙리스트 키로 등록
- TTL = Access Token 남은 시간 (만료 후 Redis 자동 삭제 — 메모리 부담 적음)
- 매 요청 JWT 검증 후 JTI 블랙리스트 체크 (Redis GET 1번)

권한 회수 ≠ 강제 로그아웃. 둘은 다른 작업.

### 2-5. 화면 동기화

| 방식 | 채택 |
|---|---|
| 자연 새로고침 (다음 페이지 이동 시 권한 재반영) | ✅ |
| 단기 폴링 | ❌ 불필요한 비용 |
| WebSocket / SSE Push | ❌ Phase 2 검토 |

Thymeleaf 서버 렌더링 + 자연 새로고침이 표준. SPA 라면 라우터 변경 시 권한 재로딩.

### 2-6. AUTH_TYPE 활용 범위

| 활용 | 채택 |
|---|---|
| UI 권한 매트릭스 화면의 분류/그룹핑/색상 | ✅ |
| 권한 판정 로직 (`hasAuthority(...)`) | ❌ (AUTH_CODE 만 사용) |

### 2-7. CM_USER_AUTH 시드 정책

- `03-seed-codes.sql` 에 시드 없음 (ADR-009 §2-5 그대로)
- 5단계 (관리자 도메인) 의 ApplicationRunner 가 admin 생성 시 **샘플 1행 GRANT** 등록 (학습 데모 목적)
- 운영 데이터는 화면에서 직접 등록

### 2-8. JWT 도입 시점 (재확인)

**Phase 1 마지막 유지** (8단계 마무리 후 별도 사이클). 변경 없음.

작업 사이클 중 임시 인증:
- 본 사이클 (권한 모델) 작업 중엔 `permitAll` 상태에서 진행
- 5단계 작업 시 `X-User-Id: {userId}` 헤더로 admin 시뮬레이션 — JWT 도입 시 제거. 운영 코드 진입 금지.

---

## 3. Consequences

### 3-1. Positive

- 매 요청 권한 판정 = Redis GET 1번 (DB 부담 최소화)
- 권한 변경 SLA 명확 — "다음 요청부터 즉시" 보장
- 운영 정책 ADR 로 명문화 → 5단계 구현 시 모호함 ↓
- 강제 로그아웃 / 라이브 동기화는 적시 (JWT 사이클 / Phase 2) 로 이연 — YAGNI

### 3-2. Negative

- 역할 권한 변경 시 SCAN + DEL 비용 (백오피스 학습 프로젝트라 사용자 수 적어 부담 미미)
- 화면의 라이브 동기화 없음 — 사용자가 새로고침 필요. 학습 단순화 트레이드오프

---

## 4. 비채택 대안

| 대안 | 비채택 사유 |
|---|---|
| 분리 키 (역할 + 사용자 override) | 매 요청 GET 2회 + 학습 단순화 |
| JWT Claims 에 권한 직접 박기 (짧은 TTL) | 권한 변경 즉시성 약함 + 토큰 비대 |
| WebSocket 라이브 동기화 | 학습 단순화 — Phase 2 (포털) 검토 |
| 화면 단기 폴링 | 불필요한 비용 |
| 강제 로그아웃 본 사이클 구현 | JWT 기능 부재 — 의미 없음. JWT 도입 사이클에서 |

---

## 5. 후속 작업

- 5단계 (관리자 도메인) 에서 `PermissionService` + `PermissionCacheService` 구현
- 5단계 작업 중 `X-User-Id` 헤더 시뮬레이션 도입 (임시)
- JWT 도입 사이클에서:
  - Access Token JTI 블랙리스트 (강제 로그아웃)
  - `X-User-Id` 시뮬레이션 제거
- Phase 2 검토: WebSocket 라이브 동기화 필요성
