# 한↔영 단어 사전 (rental-crm 도메인)

> 신규 컬럼·클래스·메서드 명명 시 한글 단어 → 영문 단어 변환의 진실의 원천.

---

## 도메인 핵심 명사

| 한글 | 영문 | 약어 (사용 시) | 비고 |
|---|---|---|---|
| 고객 | CUSTOMER | CUST | 렌탈 서비스 이용자 (B2C) |
| 관리자 | USER | - | 백오피스 사용자 (`CM_USER`) |
| 권한 / 역할 | ROLE | - | `CM_ROLE` |
| 메뉴 | MENU | - | 백오피스 메뉴 트리 |
| 알림 | NOTIFICATION | NOTI | `CM_NOTIFICATION` |
| 공통코드 | CODE | - | `CM_CODE` |
| 장비 | EQUIPMENT | EQUIP | `CT_EQUIPMENT` (가전/가구/IT/의료) |
| 상품 | PRODUCT | PROD | 장비 + 월렌탈료 조합 |
| 계약 | CONTRACT | CT | `CT_CONTRACT` |
| 기사 | ENGINEER | ENG | 설치/점검/수거 기사 |
| 방문 | VISIT | - | `CT_VISIT` |
| 청구 | BILLING | BILL | `BL_BILLING` |
| 수납 | PAYMENT | PAY | `BL_PAYMENT` |
| 연체 | OVERDUE | - | `BL_OVERDUE` |
| 배치 | BATCH | - | `BL_BATCH_LOG` |
| 이력 | LOG / HISTORY | HST | 배치/감사 이력 |

---

## 도메인 핵심 동사

| 한글 | 영문 | 비고 |
|---|---|---|
| 등록 | REGISTER / CREATE | 신규 INSERT |
| 수정 | UPDATE / MODIFY | |
| 조회 | FIND / GET / SELECT | |
| 삭제 | DELETE / REMOVE | 소프트 삭제는 INVALIDATE |
| 해지 | TERMINATE | 계약 해지 |
| 일시정지 | SUSPEND | 계약 일시정지 |
| 재개 | RESUME | 정지된 계약 재개 |
| 배정 | ASSIGN | 기사 방문 배정 |
| 완료 | COMPLETE | 방문/수납 완료 |
| 취소 | CANCEL | |
| 발급 | ISSUE | 청구서 발급 (= 청구 생성) |
| 수납 | RECEIVE | 수납 처리 |
| 결제 | PAY | 카드 결제 |
| 검증 | VERIFY / VALIDATE | Toss 결제 검증 등 |
| 발행 | PUBLISH | Kafka 이벤트 발행 |
| 소비 | CONSUME | Kafka 이벤트 소비 |
| 잠금 | LOCK | 계정 잠금 |
| 해제 | UNLOCK / RESOLVE | 잠금/연체 해제 |
| 승인 | APPROVE | (학습 범위 외) |
| 집계 | AGGREGATE / SUMMARIZE | 대시보드 통계 |

---

## 상태/플래그 단어

| 한글 | 영문 | 코드값 |
|---|---|---|
| 정상 / 활성 | ACTIVE | `ACTIVE` |
| 일시정지 | SUSPENDED | `SUSPENDED` |
| 해지됨 | TERMINATED | `TERMINATED` |
| 미납 | UNPAID | `UNPAID` |
| 연체 | OVERDUE | `OVERDUE` |
| 수납완료 | PAID | `PAID` |
| 취소됨 | CANCELLED | `CANCELLED` |
| 실행중 | RUNNING | `RUNNING` |
| 완료 | COMPLETED | `COMPLETED` |
| 실패 | FAILED | `FAILED` |
| 사용중 | Y | `USE_YN='Y'` |
| 미사용 | N | `USE_YN='N'` |
| 읽음 | Y | `READ_YN='Y'` |

---

## 외부 시스템 / 라이브러리 약어

| 한글 | 영문 / 약어 | 비고 |
|---|---|---|
| 토스페이먼츠 | TOSS | `TOSS_PAYMENT_KEY`, `TOSS_ORDER_ID` |
| 카프카 | KAFKA | 토픽 prefix `rental.*` |
| 레디스 | REDIS | |
| 엑셀 | EXCEL | |

---

## 사용 원칙

1. **풀네임 우선**. 약어는 GDI 컨벤션 대비 학습 가독성 우선이라 의도적으로 풀네임 채택
2. 단, 너무 긴 경우(`CUSTOMER` 7자) 도 풀네임 사용 — 일관성 유지
3. 신규 단어가 필요하면 본 사전에 먼저 추가 → ADR 또는 PR 설명에 근거 기록

---

## 변경 이력

- 2026-05-11: 신규 작성 — 산출물 01~08, ERD 06 의 컬럼·도메인 단어 추출
