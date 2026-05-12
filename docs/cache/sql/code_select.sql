-- =============================================================================
-- code_select.sql
-- 공통코드 시드 데이터 캐시 (조회 전용)
-- 대상: CM_CODE_GROUP / CM_CODE / CM_ROLE / CM_AUTH (마스터 + 시드 데이터)
-- 운영 데이터 (CM_USER 등) 는 제외 — 변경 빈도 높음.
-- =============================================================================

ALTER SESSION SET CURRENT_SCHEMA = rental;

SET PAGESIZE 0
SET LINESIZE 300
SET FEEDBACK OFF
SET HEADING OFF
SET TRIMSPOOL ON
SET ECHO OFF
SET VERIFY OFF
WHENEVER SQLERROR EXIT FAILURE

PROMPT === CODE GROUPS ===
PROMPT GROUP_CODE|GROUP_NAME|DESCRIPTION|USE_YN
SELECT GROUP_CODE || '|'
       || GROUP_NAME || '|'
       || NVL(DESCRIPTION, '') || '|'
       || USE_YN
  FROM CM_CODE_GROUP
 ORDER BY GROUP_CODE;

PROMPT
PROMPT === CODES ===
PROMPT GROUP_CODE|CODE_VALUE|CODE_NAME|SORT_ORDER|USE_YN
SELECT GROUP_CODE || '|'
       || CODE_VALUE || '|'
       || CODE_NAME || '|'
       || SORT_ORDER || '|'
       || USE_YN
  FROM CM_CODE
 ORDER BY GROUP_CODE, SORT_ORDER, CODE_VALUE;

PROMPT
PROMPT === ROLES ===
PROMPT ROLE_ID|ROLE_CODE|ROLE_NAME|USE_YN
SELECT ROLE_ID || '|'
       || ROLE_CODE || '|'
       || ROLE_NAME || '|'
       || USE_YN
  FROM CM_ROLE
 ORDER BY ROLE_ID;

PROMPT
PROMPT === AUTHS ===
PROMPT AUTH_CODE|AUTH_NAME|MENU_ID|AUTH_TYPE|SORT_ORDER|USE_YN
SELECT AUTH_CODE || '|'
       || AUTH_NAME || '|'
       || NVL(TO_CHAR(MENU_ID), '') || '|'
       || AUTH_TYPE || '|'
       || SORT_ORDER || '|'
       || USE_YN
  FROM CM_AUTH
 ORDER BY MENU_ID NULLS LAST, SORT_ORDER, AUTH_CODE;

PROMPT
PROMPT === MENUS ===
PROMPT MENU_ID|PARENT|DEPTH|MENU_NAME|MENU_TYPE|MENU_URL
SELECT MENU_ID || '|'
       || NVL(TO_CHAR(PARENT_MENU_ID), '') || '|'
       || MENU_DEPTH || '|'
       || MENU_NAME || '|'
       || MENU_TYPE || '|'
       || NVL(MENU_URL, '')
  FROM CM_MENU
 ORDER BY MENU_DEPTH, SORT_ORDER, MENU_ID;

EXIT;
