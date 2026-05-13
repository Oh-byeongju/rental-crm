-- ============================================================
-- rental-crm 성능 측정 시드 (ADR-014 Step 7-A)
--
-- Ch.1 청구 배치 6 라운드 측정 입력:
--   CT_EQUIPMENT 5  (각 EQUIPMENT_TYPE 1)
--   CT_PRODUCT   5  (장비 1:1)
--   CT_CUSTOMER  10,000
--   CT_CONTRACT  50,000  (1만 customer × 평균 5 contract — round-robin)
--
-- 모든 행 ACTIVE — BILLING_CREATE 가 5만건 INSERT 대상.
-- Oracle CONNECT BY LEVEL + INSERT INTO ... SELECT 패턴 (PL/SQL FORALL 보다 단순).
--
-- 컨테이너 최초 부팅 (또는 down -v 후 재기동) 시 03 다음에 자동 실행.
-- ============================================================

ALTER SESSION SET CURRENT_SCHEMA = rental;

SET DEFINE OFF;
SET SERVEROUTPUT ON;

PROMPT 04-seed-perf-data.sql 시작 — Ch.1 측정 데이터 (1만 customer + 5만 contract)

-- ============================================================
-- 1. CT_EQUIPMENT — 5 행 (각 EQUIPMENT_TYPE 1개)
-- ============================================================

INSERT INTO CT_EQUIPMENT (EQUIPMENT_ID, EQUIPMENT_CODE, EQUIPMENT_TYPE, MODEL_NAME, MANUFACTURER, RELEASE_DATE, DESCRIPTION, STOCK_QTY, USE_YN, FIRS_REG_PGM_ID, FIRS_REG_USER_ID, FIRS_REG_IP, FINA_REG_PGM_ID, FINA_REG_USER_ID, FINA_REG_IP)
VALUES (SEQ_CT_EQUIPMENT.NEXTVAL, 'EQ-PERF-AC',  'AC',        '에어컨 PerfModel',  'PerfMaker', DATE '2025-01-01', '측정용 시드', 999999, 'Y', 'SEED', 'SYSTEM', '127.0.0.1', 'SEED', 'SYSTEM', '127.0.0.1');

INSERT INTO CT_EQUIPMENT (EQUIPMENT_ID, EQUIPMENT_CODE, EQUIPMENT_TYPE, MODEL_NAME, MANUFACTURER, RELEASE_DATE, DESCRIPTION, STOCK_QTY, USE_YN, FIRS_REG_PGM_ID, FIRS_REG_USER_ID, FIRS_REG_IP, FINA_REG_PGM_ID, FINA_REG_USER_ID, FINA_REG_IP)
VALUES (SEQ_CT_EQUIPMENT.NEXTVAL, 'EQ-PERF-FUR', 'FURNITURE', '소파 PerfModel',    'PerfMaker', DATE '2025-01-01', '측정용 시드', 999999, 'Y', 'SEED', 'SYSTEM', '127.0.0.1', 'SEED', 'SYSTEM', '127.0.0.1');

INSERT INTO CT_EQUIPMENT (EQUIPMENT_ID, EQUIPMENT_CODE, EQUIPMENT_TYPE, MODEL_NAME, MANUFACTURER, RELEASE_DATE, DESCRIPTION, STOCK_QTY, USE_YN, FIRS_REG_PGM_ID, FIRS_REG_USER_ID, FIRS_REG_IP, FINA_REG_PGM_ID, FINA_REG_USER_ID, FINA_REG_IP)
VALUES (SEQ_CT_EQUIPMENT.NEXTVAL, 'EQ-PERF-IT',  'IT',        '노트북 PerfModel',  'PerfMaker', DATE '2025-01-01', '측정용 시드', 999999, 'Y', 'SEED', 'SYSTEM', '127.0.0.1', 'SEED', 'SYSTEM', '127.0.0.1');

INSERT INTO CT_EQUIPMENT (EQUIPMENT_ID, EQUIPMENT_CODE, EQUIPMENT_TYPE, MODEL_NAME, MANUFACTURER, RELEASE_DATE, DESCRIPTION, STOCK_QTY, USE_YN, FIRS_REG_PGM_ID, FIRS_REG_USER_ID, FIRS_REG_IP, FINA_REG_PGM_ID, FINA_REG_USER_ID, FINA_REG_IP)
VALUES (SEQ_CT_EQUIPMENT.NEXTVAL, 'EQ-PERF-MED', 'MEDICAL',   '안마기 PerfModel',  'PerfMaker', DATE '2025-01-01', '측정용 시드', 999999, 'Y', 'SEED', 'SYSTEM', '127.0.0.1', 'SEED', 'SYSTEM', '127.0.0.1');

INSERT INTO CT_EQUIPMENT (EQUIPMENT_ID, EQUIPMENT_CODE, EQUIPMENT_TYPE, MODEL_NAME, MANUFACTURER, RELEASE_DATE, DESCRIPTION, STOCK_QTY, USE_YN, FIRS_REG_PGM_ID, FIRS_REG_USER_ID, FIRS_REG_IP, FINA_REG_PGM_ID, FINA_REG_USER_ID, FINA_REG_IP)
VALUES (SEQ_CT_EQUIPMENT.NEXTVAL, 'EQ-PERF-AC2', 'AC',        '에어컨 PerfModel2', 'PerfMaker', DATE '2025-01-01', '측정용 시드', 999999, 'Y', 'SEED', 'SYSTEM', '127.0.0.1', 'SEED', 'SYSTEM', '127.0.0.1');

COMMIT;

-- ============================================================
-- 2. CT_PRODUCT — 5 행 (장비 1:1, 다양한 MONTHLY_FEE)
-- ============================================================

INSERT INTO CT_PRODUCT (PRODUCT_ID, PRODUCT_CODE, EQUIPMENT_ID, PRODUCT_NAME, MONTHLY_FEE, CONTRACT_MONTHS, DEPOSIT_AMOUNT, INSTALL_FEE, DESCRIPTION, USE_YN, FIRS_REG_PGM_ID, FIRS_REG_USER_ID, FIRS_REG_IP, FINA_REG_PGM_ID, FINA_REG_USER_ID, FINA_REG_IP)
SELECT SEQ_CT_PRODUCT.NEXTVAL,
       'PR-PERF-' || EQUIPMENT_CODE,
       EQUIPMENT_ID,
       '측정상품 ' || EQUIPMENT_CODE,
       CASE EQUIPMENT_CODE
           WHEN 'EQ-PERF-AC'  THEN 30000
           WHEN 'EQ-PERF-FUR' THEN 50000
           WHEN 'EQ-PERF-IT'  THEN 70000
           WHEN 'EQ-PERF-MED' THEN 90000
           ELSE                    40000
       END,
       36, 0, 0, '측정용 시드', 'Y',
       'SEED', 'SYSTEM', '127.0.0.1', 'SEED', 'SYSTEM', '127.0.0.1'
  FROM CT_EQUIPMENT
 WHERE EQUIPMENT_CODE LIKE 'EQ-PERF-%';

COMMIT;

-- ============================================================
-- 3. CT_CUSTOMER — 1만 행
-- CONNECT BY LEVEL 로 행 생성. PASSWORD 는 placeholder bcrypt 해시.
-- ============================================================

INSERT INTO CT_CUSTOMER (CUSTOMER_ID, CUSTOMER_NO, CUSTOMER_NAME, EMAIL, PASSWORD, PHONE, BIRTH_DATE, ADDRESS_ZIP, ADDRESS, TERMS_AGREE_YN, USE_YN, LOGIN_FAIL_CNT, FIRS_REG_PGM_ID, FIRS_REG_USER_ID, FIRS_REG_IP, FINA_REG_PGM_ID, FINA_REG_USER_ID, FINA_REG_IP)
SELECT SEQ_CT_CUSTOMER.NEXTVAL,
       'CUS-PERF-' || LPAD(lvl, 6, '0'),
       'PerfUser' || lvl,
       'perf' || lvl || '@test.local',
       '$2a$10$placeholder.hash.for.perf.seed.do.not.use.for.login.aaaaa',  -- 학습용 placeholder
       '010-' || LPAD(MOD(lvl, 10000), 4, '0') || '-' || LPAD(MOD(lvl * 7, 10000), 4, '0'),
       DATE '1980-01-01' + MOD(lvl, 14600),  -- 1980~2020 분포
       LPAD(MOD(lvl, 100000), 5, '0'),
       '서울시 측정구 PerfDong ' || lvl,
       'Y', 'Y', 0,
       'SEED', 'SYSTEM', '127.0.0.1', 'SEED', 'SYSTEM', '127.0.0.1'
  FROM (SELECT LEVEL AS lvl FROM DUAL CONNECT BY LEVEL <= 10000);

COMMIT;

-- ============================================================
-- 4. CT_CONTRACT — 5만 행 (1만 customer × 평균 5 contract — round-robin)
--
-- CUSTOMER_ID round-robin: customer_seed 의 시작 ID 모르므로 MIN+offset 사용.
-- PRODUCT_ID  round-robin: PR-PERF-* 5개에서 modulo 분배.
-- CONTRACT_NO 는 'CT-PERF-NNNNNN' 형식 (운영 자동 채번 'CT-YYYYMMDD-NNNNN' 과 prefix 로 구분).
-- ============================================================

DECLARE
    v_min_cust   NUMBER;
    v_min_prod   NUMBER;
BEGIN
    SELECT MIN(CUSTOMER_ID) INTO v_min_cust FROM CT_CUSTOMER WHERE CUSTOMER_NO LIKE 'CUS-PERF-%';
    SELECT MIN(PRODUCT_ID)  INTO v_min_prod FROM CT_PRODUCT  WHERE PRODUCT_CODE LIKE 'PR-PERF-%';

    INSERT INTO CT_CONTRACT (CONTRACT_ID, CONTRACT_NO, CUSTOMER_ID, PRODUCT_ID, MONTHLY_FEE, START_DATE, END_DATE, INSTALL_ADDRESS, CONTRACT_STATUS, FIRS_REG_PGM_ID, FIRS_REG_USER_ID, FIRS_REG_IP, FINA_REG_PGM_ID, FINA_REG_USER_ID, FINA_REG_IP)
    SELECT SEQ_CT_CONTRACT.NEXTVAL,
           'CT-PERF-' || LPAD(lvl, 6, '0'),
           v_min_cust + MOD(lvl - 1, 10000),                      -- 0..9999 round-robin
           v_min_prod + MOD(lvl - 1, 5),                          -- 0..4 round-robin (PR-PERF-* 5개)
           30000 + MOD(lvl, 70) * 1000,                           -- 30k~99k 분포
           DATE '2024-01-01' + MOD(lvl, 365),
           DATE '2027-01-01' + MOD(lvl, 365),                     -- 3년 + alpha
           '서울시 측정구 PerfDong 설치주소 ' || lvl,
           'ACTIVE',
           'SEED', 'SYSTEM', '127.0.0.1', 'SEED', 'SYSTEM', '127.0.0.1'
      FROM (SELECT LEVEL AS lvl FROM DUAL CONNECT BY LEVEL <= 50000);

    COMMIT;
END;
/

-- ============================================================
-- 5. 완료 보고
-- ============================================================
DECLARE
    v_eq   NUMBER; v_prod NUMBER; v_cust NUMBER; v_ctr  NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_eq   FROM CT_EQUIPMENT WHERE EQUIPMENT_CODE LIKE 'EQ-PERF-%';
    SELECT COUNT(*) INTO v_prod FROM CT_PRODUCT   WHERE PRODUCT_CODE   LIKE 'PR-PERF-%';
    SELECT COUNT(*) INTO v_cust FROM CT_CUSTOMER  WHERE CUSTOMER_NO    LIKE 'CUS-PERF-%';
    SELECT COUNT(*) INTO v_ctr  FROM CT_CONTRACT  WHERE CONTRACT_NO    LIKE 'CT-PERF-%';
    DBMS_OUTPUT.PUT_LINE('rental-crm perf seed: equipment=' || v_eq
                         || ', product=' || v_prod
                         || ', customer=' || v_cust
                         || ', contract=' || v_ctr);
END;
/
