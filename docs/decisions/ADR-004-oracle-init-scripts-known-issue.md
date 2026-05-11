# ADR-004 — Oracle init-scripts 자동 실행 알려진 이슈 + Fallback 메커니즘

- **작성일**: 2026-05-11
- **상태**: 알려진 이슈 — 우회 완료
- **연관 파일**: `infra/docker-compose.yml`, `infra/init-scripts/oracle/*.sql`, `infra/start.bat`

---

## 1. 발견 경위

ERD DDL 작성 후 `docker compose up -d` 로 첫 인프라 기동 시 다음 3가지 시나리오를 거침:

| 시도 | 이미지 | 환경변수 | 결과 |
|---|---|---|---|
| 1차 | `gvenzl/oracle-xe:21-slim-faststart` | `ORACLE_DATABASE=XEPDB1`, `APP_USER=rental` | ❌ init scripts 미실행 |
| 2차 | `gvenzl/oracle-xe:21-slim` | `ORACLE_DATABASE=XEPDB1`, `APP_USER=rental` | ❌ ORA-65012 로 setup 중단 |
| 3차 | `gvenzl/oracle-xe:21-slim` | `APP_USER=rental` (`ORACLE_DATABASE` 제거) | ⚠️ `docker logs` 는 `running ... DONE` 출력했으나 RENTAL 스키마에 객체 0개 |

3차에서 결국 `docker exec` 로 SYSTEM 계정에서 동일 SQL 파일을 수동 실행 → 정상 작동 확인.

---

## 2. 원인 분석

### 1차 — faststart 이미지
- `gvenzl/oracle-xe:21-slim-faststart` 는 미리 초기화된 DB 가 이미지에 포함되어 있는 변형 (40초 만에 healthy)
- "DB 가 이미 초기화됨" 으로 판단해 `init-scripts/` 의 SQL 을 **자동 실행하지 않음**
- `APP_USER` 환경변수도 동일하게 무시됨

→ **결정**: faststart 변형 비채택. 표준 `21-slim` 사용.

### 2차 — ORACLE_DATABASE 환경변수 충돌
- `gvenzl/oracle-xe` 의 기본 PDB 는 이미 `XEPDB1`
- `ORACLE_DATABASE=XEPDB1` 명시 시 **새 PDB 생성 시도** → 이름 충돌 → `ORA-65012: Pluggable database XEPDB1 already exists`
- 이 에러로 entrypoint 스크립트가 중단되어 이후 `APP_USER` 생성과 `init-scripts` 실행이 모두 스킵됨

→ **결정**: `ORACLE_DATABASE` 환경변수 제거. 기본 PDB `XEPDB1` 그대로 사용. JDBC URL 에서 `//host:1521/XEPDB1` 로 접속.

### 3차 — 자동 실행 시 객체 미생성 (미해결 미스터리)
- 1·2차 원인 제거 후 시도
- `docker logs rental-oracle` 에는 다음 출력 정상 표시:
  ```
  CONTAINER: Executing user defined scripts...
  CONTAINER: running /container-entrypoint-initdb.d/01-create-schema.sql ... DONE
  CONTAINER: running /container-entrypoint-initdb.d/02-create-indexes.sql ... DONE
  CONTAINER: running /container-entrypoint-initdb.d/03-seed-codes.sql ... DONE
  ```
- 그러나 RENTAL 스키마에는 객체 0 개, SYSTEM 스키마에도 CM_/CT_/BL_ 접두사 객체 0 개
- 동일 SQL 을 수동(`docker exec -i ... sqlplus system/oracle@//...XEPDB1 < /container-entrypoint-initdb.d/01-create-schema.sql`)으로 실행 시 정상 생성 확인

**추정 원인** (검증되지 않음):
- gvenzl 의 init-scripts 실행 메커니즘이 SQL 을 어느 PDB / 어느 계정으로 실행하는지 불투명
- DONE 출력이 실제 성공을 의미하지 않을 수 있음 (silently swallow)
- 우리 SQL 의 `ALTER SESSION SET CURRENT_SCHEMA = rental` 명령이 자동 실행 컨텍스트에서 다르게 처리될 가능성

→ **결정**: 원인 추적은 우선순위 낮음 (학습 프로젝트). **수동 fallback 메커니즘 도입**.

---

## 3. Fallback 메커니즘

### 3-1. `start.bat` 보강

`docker compose up -d` 직후 다음 검증/복구 단계 추가:

1. Oracle healthcheck 가 healthy 도달까지 대기 (최대 10분)
2. `sqlplus rental/rental@//localhost:1521/XEPDB1` 로 `SELECT COUNT(*) FROM USER_TABLES;` 조회
3. 결과 0 이면 **fallback 수동 실행**:
   - SYSTEM 계정으로 `01-create-schema.sql` / `02-create-indexes.sql` / `03-seed-codes.sql` 직접 실행
   - 진행 상황 화면 출력
4. 결과 > 0 이면 정상 init 완료로 간주 → 건너뜀

이 방식은:
- **첫 실행** (자동 init 실패) → fallback 작동 → 정상 상태 복구
- **재실행** (이미 데이터 있음) → fallback 스킵 → 빠르게 시작
- **`reset.bat` 후 첫 실행** → fallback 작동 (clean slate 라 자동 init 실패 가능성 동일)

### 3-2. init-scripts/oracle/ 파일은 그대로 유지

자동 실행이 (이론적으로는) 작동해야 할 표준 위치이므로 파일은 유지.
다음 gvenzl 이미지 업데이트나 메커니즘 개선 시 자동 실행이 작동할 수 있음 → 그때 fallback 은 자연스럽게 스킵됨.

---

## 4. Consequences

### 긍정
- 모든 사용자가 클론 + `start.bat` 더블클릭으로 정상 환경 구축 가능 (자동 init 실패해도)
- 자동 init 가 작동하면 더 빠르게 끝남 (fallback 스킵)
- 학습 프로젝트의 본 목표(ERD/JPA/배치/Kafka)에 영향 없음

### 부정 / 비용
- 첫 실행 시 fallback 실행 추가 시간 (~30초)
- start.bat 코드 복잡도 증가 (~50줄 추가)
- 자동 init 미스터리는 미해결로 남음 → 다음 gvenzl 버전 변경 시 재발견 가능

---

## 5. 검증 명령

수동 실행 결과 (2026-05-11 확정):

```
Tables:      17  ✅
Sequences:   16  ✅  (CM_CODE_GROUP은 VARCHAR PK라 시퀀스 없음)
Indexes:     23  ✅  (IDX_* 접두사)
PKs:         17  ✅
UKs:         17  ✅
FKs:         18  ✅
Checks:      16  ✅  (SYS_C* 제외)
Code groups:  6  ✅
Codes:       22  ✅  (4+3+4+4+3+4)
Roles:        3  ✅
```

---

## 6. 추후 모니터링

- gvenzl/oracle-xe 다음 버전(또는 oracle-free 마이그레이션) 시 자동 init 작동 여부 재검증
- 만약 자동 init 가 작동하면 fallback 스크립트는 효과적으로 사라짐 (조건문에서 항상 false)
- 이때 ADR-004 를 "해결됨" 으로 갱신 가능

---

## 7. 관련 ADR

- ADR-001: ERD CM_* 블록 — 객체 정의
- ADR-002: ERD CT_* 블록 — 객체 정의
- ADR-003: ERD BL_* 블록 — 객체 정의
