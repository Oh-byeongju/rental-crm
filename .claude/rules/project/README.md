# 현재 프로젝트 매핑 (식별자 표)

> 이식 시 이 표만 보면 변경 포인트가 명확. **항상 로드되는 헌법** 의 일부.

---

## 매핑 표

| 항목 | 값 |
|---|---|
| 프로젝트명 | **rental-crm** (장비 렌탈 청구 관리 시스템) |
| 도메인 | 장비 렌탈 (가전 / 가구 / IT장비 / 의료장비) |
| 백엔드 프레임워크 | Spring Boot 3.x |
| ORM | Spring Data JPA (Hibernate) |
| DB | Oracle 21c (Docker — `gvenzl/oracle-free`) |
| 메시지 브로커 | Apache Kafka (Spring Kafka) |
| 캐시 | Redis (Spring Data Redis) |
| 백오피스 UI | Thymeleaf + AdminLTE + DataTables.js |
| 고객 포털 UI | React 18 + Zustand + Axios + AntD/MUI |
| 결제 | Toss Payments |
| 컨테이너 | Docker + docker-compose (infra / backend / frontend 레이어 분리) |
| CI/CD | GitHub Actions |
| 트랜잭션 매니저 | `@Transactional` (기본) |
| 패키지 루트 | `com.rental.crm` (확정 시 갱신) |
| 테이블 접두사 | `CM_*` (공통/권한/알림), `CT_*` (계약/장비/고객), `BL_*` (청구/수납/배치) |
| 청구상태 코드 도메인 | `BILLING_STATUS` (UNPAID / OVERDUE / PAID / CANCELLED) |
| 계약상태 코드 도메인 | `CONTRACT_STATUS` (ACTIVE / SUSPENDED / TERMINATED) |
| Kafka 토픽 prefix | `rental.*` (예: `rental.billing.created`) |

---

## `.claude/rules/project/` 잔존 — 항상 로드 (헌법)

| 파일 | 영역 |
|---|---|
| [`README.md`](README.md) | 본 매핑 표 (이 파일) |
| [`general/system-info.md`](general/system-info.md) | 빌드 명령·기술 스택·아키텍처·환경 프로파일 |
| [`db/business-domain.md`](db/business-domain.md) | 업무 도메인 prefix → 의미 매핑 (테이블 / 시퀀스 / 패키지 설계 시 참조) |

---

## 작업 시점 룰 위치

다음 룰들은 자동 로드되지 않고 작업 시점에 description 매칭으로 Read.

| 영역 | 위치 |
|---|---|
| 전역 룰 (모든 도메인 적용) | [`docs/global-rules/`](../../../docs/global-rules/) |
| 백엔드 로컬 룰 | [`backend/guide/`](../../../backend/guide/) |
| 고객 포털 로컬 룰 | [`customer-portal/guide/`](../../../customer-portal/guide/) |
| 인프라 로컬 룰 | [`infra/guide/`](../../../infra/guide/) |

---

## 이식 시 변경 포인트

본 폴더 + `{도메인}/guide/` 안의 룰들은 다음 정보를 담고 있음 — 새 프로젝트로 이식 시 수정 필요:

- **프로젝트명·도메인**
- **패키지 루트** (예: `com.rental.crm`)
- **테이블 접두사** (예: `CM_*` / `CT_*` / `BL_*`)
- **코드 도메인** (예: `BILLING_STATUS`, `CONTRACT_STATUS`)
- **Kafka 토픽 prefix** (예: `rental.*`)
- **DB 종류** (Oracle 21c)

## 신규 룰 추가 절차

상세 절차: [`@.claude/rules/_meta/rule-management.md`](../_meta/rule-management.md)
