# R5 UNDO 폭주 — 인프라 사전 조사 + 측정 설계 (재정의)

작성일: 2026-05-17 (4회차, ADR-014 Step 7-C-3 사전 단계)
환경: Windows 10 Pro / Docker Desktop / `gvenzl/oracle-xe:21-slim` / Oracle 21c XE
대상: 활성 계약 50,000 건 → 청구 50,000 건 INSERT (R5 candidate month = 2026-05)

> 본 문서는 **측정 리포트가 아니라 R5 측정 전 인프라 조사 + 설계 확정** 노트.
> 측정 결과는 측정 후 `2026-05-NN-billing-create-...-r5.md` 로 별도 작성.
> 선행 컨텍스트: [`2026-05-13-billing-create-r1-r2-r3-r4.md`](2026-05-13-billing-create-r1-r2-r3-r4.md) §4.

---

## 1. 핵심 결론

| 항목 | 결론 |
|---|---|
| UNDO 작게 강제 방법 | XEPDB1 에 작은 UNDO TS 생성 + `ALTER SYSTEM SET UNDO_TABLESPACE` 전환 (SYSDBA) |
| 자동화 위치 | startdb.d 부적합 → **측정 절차 내 수동 SQL 2쌍** (전환→측정→원복) |
| ⚠️ 설계 보정 | "5MB 고정" → **"R1 ORA-30036 트리거 / R4 통과 임계 크기 탐색"** 으로 재정의 |

---

## 2. UNDO tablespace 작게 강제 — 확정 절차

Oracle 21c XE = **LOCAL UNDO 기본** (12.2+). XEPDB1 PDB 가 자체 UNDO TS 보유.

확인 쿼리:
```sql
SELECT property_value FROM database_properties
WHERE property_name = 'LOCAL_UNDO_ENABLED';   -- 'TRUE' 예상
```

접속 (SYSDBA — `rental` 유저로는 UNDO TS 조작 불가):
```bash
docker exec -i rental-oracle sqlplus sys/oracle@//localhost:1521/XEPDB1 as sysdba
```
> 비번 `oracle` = `infra/.env` 의 `ORACLE_PASSWORD` 기본값. `.env` 변경 시 그 값 사용.

작게 전환:
```sql
ALTER SESSION SET CONTAINER = XEPDB1;
-- OMF 활성 시 (XE 기본: db_create_file_dest=/opt/oracle/oradata) 경로 생략 가능
CREATE UNDO TABLESPACE undo_small DATAFILE SIZE 2M AUTOEXTEND OFF;
-- OMF 비활성이면 경로 명시:
-- CREATE UNDO TABLESPACE undo_small
--   DATAFILE '/opt/oracle/oradata/XE/XEPDB1/undo_small01.dbf' SIZE 2M AUTOEXTEND OFF;
ALTER SYSTEM SET UNDO_TABLESPACE = undo_small;
```

측정 후 원복:
```sql
ALTER SESSION SET CONTAINER = XEPDB1;
ALTER SYSTEM SET UNDO_TABLESPACE = UNDOTBS1;
DROP TABLESPACE undo_small INCLUDING CONTENTS AND DATAFILES;
```

> `AUTOEXTEND OFF` 가 핵심. 켜져 있으면 datafile 이 자동 확장돼 ORA-30036 이 안 뜸.

---

## 3. 자동화 위치 결정

| 위치 | 동작 | R5 적합성 |
|---|---|---|
| `/container-entrypoint-initdb.d/` | 최초 1회 (볼륨 빈 경우) — 이미 채워짐 | ❌ 실행 안 됨 |
| `/container-entrypoint-startdb.d/` | **DB (재)시작마다** 실행 | ❌ R1~R4/R6 까지 작은 UNDO 오염 |
| **측정 절차 내 수동 SQL 2쌍** | 측정 직전 전환 / 직후 원복 | ✅ 통제됨, 다른 라운드 무오염 |

→ `infra/init-scripts/oracle/` 에 스크립트 추가하지 않음. 측정 절차 문서에 SQL 박는다.

---

## 4. ⚠️ 설계 보정 — INSERT 의 UNDO 는 극소

[99_업무현황.md:209](../99_업무현황.md#L209) 의 *"UNDO 5MB → R1 single-save → ORA-30036 예상"* 은 그대로면 **재현이 안 될 공산이 큼**:

- INSERT 의 undo 레코드 = "이 rowid 를 delete" 한 줄. DELETE/UPDATE 는 원본 값 전체 보존이라 훨씬 큼.
- 순수 INSERT 5만 건 UNDO 총량이 5MB 를 안 넘기면 R1 도 통과 → **"R1 실패 / R4 성공" 학습 대비 자체가 성립 안 함**.

### 재정의된 R5 — 탐색적 임계 탐지

목적은 절대 크기가 아니라 **chunk commit(R4)이 단일 tx(R1)보다 UNDO 에 강함을 입증**하는 것.

1. UNDO datafile **2MB + AUTOEXTEND OFF** 로 시작
2. **R1**(single-save, 단일 tx 5만건) 실행
   - ORA-30036 발생 → 임계 찾음, 그 크기 기록
   - 정상 종료 → UNDO 를 더 줄여(1MB → 512K …) 재시도, ORA-30036 트리거 지점 탐색
3. 그 임계 크기에서 **R4**(chunk-commit) 실행 → **정상 종료 검증** (commit 마다 UNDO 해제)
4. 결과: "R1 이 터지고 R4 가 통과하는 UNDO 크기" 자체가 학습 산출물

> ORA-30036 메시지: `ORA-30036: unable to extend segment by N in undo tablespace 'UNDO_SMALL'`
> R5 candidate month = **2026-05** (R1~R4 누적 200,000 행과 분리). 실패한 R1 은 rollback 되므로 month 오염 없음 — 단 부분 commit 되는 변형 측정 시 cleanup 주의.

---

## 5. 측정 절차 (Docker 기동 후)

```
0. infra 기동 — docker ps 로 4 컨테이너 확인 (없으면 infra/start.bat)
   ./gradlew :backoffice:bootRun  +  :batch:bootRun
1. LOCAL_UNDO 확인 쿼리 (§2)
2. undo_small 2M 생성 + 전환 (§2 "작게 전환")
3. R1 호출 (month=2026-05) → 결과 분류:
     ORA-30036  → 임계 확정, §5-4 로
     정상 종료  → undo_small DROP 후 1M 로 재생성, 2 반복 (512K 까지 단계 축소)
4. 동일 UNDO 크기 유지하고 R4 호출 (month=2026-05, 다른 round_no)
     정상 종료 기대 — chunk commit UNDO 해제 입증
5. 원복 (§2 "측정 후 원복")
6. 측정 리포트 작성 → docs/perf-reports/2026-05-NN-...-r5.md
```

> 포트 정리: bootRun 중지 후 JVM 자식 잔존 시
> `(Get-NetTCPConnection -LocalPort 9091,9093 -State Listen).OwningProcess | %{Stop-Process -Id $_ -Force}`

---

## 6. 출처

- [gvenzl/oci-oracle-xe README — initdb.d / startdb.d 메커니즘](https://github.com/gvenzl/oci-oracle-xe/blob/main/README.md)
- [Oracle-Base — Multitenant Local Undo Mode (12.2+, 21c 기본)](https://oracle-base.com/articles/12c/multitenant-local-undo-mode-12cr2)
- [Oracle Docs — UNDO_TABLESPACE 파라미터](https://docs.oracle.com/en/database/oracle/oracle-database/19/refrn/UNDO_TABLESPACE.html)
- [Hemant's Oracle DBA Blog — UNDO/REDO for INSERTs vs DELETEs (INSERT undo 극소 근거)](https://hemantoracledba.blogspot.com/2007/04/undo-and-redo-for-inserts-and-deletes.html)
