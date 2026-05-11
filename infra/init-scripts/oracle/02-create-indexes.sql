-- ============================================================
-- rental-crm 보조 인덱스 DDL
-- PK / UNIQUE 는 01-create-schema.sql 에 포함됨. 본 파일은 조회 패턴 최적화 인덱스.
--
-- 인덱스 명명: IDX_<테이블>_<용도>
-- 모든 인덱스 명세 근거: ADR-001 / ADR-002 / ADR-003
-- ============================================================

ALTER SESSION SET CURRENT_SCHEMA = rental;

-- ============================================================
-- CM_* 인덱스
-- ============================================================

-- 그룹 내 사용중 코드 정렬 조회 (가장 빈번)
CREATE INDEX IDX_CM_CODE_GROUP
    ON CM_CODE (GROUP_CODE, USE_YN, SORT_ORDER);

-- 미읽음 알림 최신순 조회
CREATE INDEX IDX_CM_NOTIFICATION_UNREAD
    ON CM_NOTIFICATION (READ_YN, FIRS_REG_DTS DESC);

-- 수신자별 알림 조회
CREATE INDEX IDX_CM_NOTIFICATION_RECIPIENT
    ON CM_NOTIFICATION (RECIPIENT_USER_ID, FIRS_REG_DTS DESC);

-- ============================================================
-- CT_* 인덱스
-- ============================================================

-- 고객명 LIKE 검색
CREATE INDEX IDX_CT_CUSTOMER_NAME
    ON CT_CUSTOMER (CUSTOMER_NAME);

-- 연락처 LIKE 검색
CREATE INDEX IDX_CT_CUSTOMER_PHONE
    ON CT_CUSTOMER (PHONE);

-- 유형별 사용중 장비 조회
CREATE INDEX IDX_CT_EQUIPMENT_TYPE
    ON CT_EQUIPMENT (EQUIPMENT_TYPE, USE_YN);

-- 장비별 사용중 상품 조회
CREATE INDEX IDX_CT_PRODUCT_EQUIPMENT
    ON CT_PRODUCT (EQUIPMENT_ID, USE_YN);

-- 고객별 계약 조회 (상태 필터)
CREATE INDEX IDX_CT_CONTRACT_CUSTOMER
    ON CT_CONTRACT (CUSTOMER_ID, CONTRACT_STATUS);

-- 월청구 배치 대상 조회 (ACTIVE 만)
CREATE INDEX IDX_CT_CONTRACT_STATUS
    ON CT_CONTRACT (CONTRACT_STATUS);

-- 만료 임박 계약 조회
CREATE INDEX IDX_CT_CONTRACT_END_DATE
    ON CT_CONTRACT (END_DATE, CONTRACT_STATUS);

-- 지역별 가용 기사 조회
CREATE INDEX IDX_CT_ENGINEER_AREA
    ON CT_ENGINEER (AREA, USE_YN);

-- 기사별 일정 조회 (5건 초과 검증 — 04 §5-1)
CREATE INDEX IDX_CT_VISIT_ENGINEER_DATE
    ON CT_VISIT (ENGINEER_ID, SCHEDULED_DATE);

-- 계약별 방문 이력
CREATE INDEX IDX_CT_VISIT_CONTRACT
    ON CT_VISIT (CONTRACT_ID, VISIT_STATUS);

-- ============================================================
-- BL_* 인덱스
-- ============================================================

-- 고객별 청구 이력 (최근 12건)
CREATE INDEX IDX_BL_BILLING_CUSTOMER
    ON BL_BILLING (CUSTOMER_ID, BILLING_MONTH DESC);

-- 연체 배치 대상 (UNPAID + DUE_DATE < SYSDATE)
CREATE INDEX IDX_BL_BILLING_STATUS_DUE
    ON BL_BILLING (BILLING_STATUS, DUE_DATE);

-- 월별 통계 / 대시보드
CREATE INDEX IDX_BL_BILLING_MONTH
    ON BL_BILLING (BILLING_MONTH, BILLING_STATUS);

-- 청구별 수납 조회
CREATE INDEX IDX_BL_PAYMENT_BILLING
    ON BL_PAYMENT (BILLING_ID, PAYMENT_STATUS);

-- 고객별 납부 이력 (최신순)
CREATE INDEX IDX_BL_PAYMENT_CUSTOMER_DATE
    ON BL_PAYMENT (CUSTOMER_ID, PAYMENT_DATE DESC);

-- 일별/수단별 통계
CREATE INDEX IDX_BL_PAYMENT_DATE_METHOD
    ON BL_PAYMENT (PAYMENT_DATE, PAYMENT_METHOD);

-- 고객별 미해결 연체 조회 (RESOLVED_AT IS NULL)
CREATE INDEX IDX_BL_OVERDUE_CUSTOMER_RESOLVED
    ON BL_OVERDUE (CUSTOMER_ID, RESOLVED_AT);

-- 장기 연체자 통계
CREATE INDEX IDX_BL_OVERDUE_DAYS
    ON BL_OVERDUE (OVERDUE_DAYS DESC);

-- 최근 배치 이력 조회
CREATE INDEX IDX_BL_BATCH_LOG_STARTED
    ON BL_BATCH_LOG (STARTED_AT DESC);

-- 실패 배치 재실행 검색
CREATE INDEX IDX_BL_BATCH_LOG_TYPE_STATUS
    ON BL_BATCH_LOG (BATCH_TYPE, BATCH_STATUS);

-- ============================================================
-- 완료 메시지
-- ============================================================
BEGIN
    DBMS_OUTPUT.PUT_LINE('rental-crm indexes created: 22 secondary indexes');
END;
/
