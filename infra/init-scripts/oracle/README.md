# Oracle 초기화 스크립트

> `gvenzl/oracle-xe` 이미지는 `/container-entrypoint-initdb.d/` 아래의 `.sql` / `.sh` 파일을
> **최초 컨테이너 부팅 시 1회 자동 실행** 한다 (볼륨이 비어 있을 때만).

---

## 실행 순서

알파벳/숫자 순서로 실행. 파일명 prefix 로 순서 제어:

```
01-create-schema.sql        ← 스키마/시퀀스/테이블 DDL (19 테이블)
02-create-indexes.sql       ← 보조 인덱스 (24 인덱스)
03-seed-codes.sql           ← 공통코드 + 메뉴 + AUTH + 역할-권한 매핑 초기 시드
04-seed-test-data.sql       ← 학습용 더미 데이터 (선택 — 미작성)
```

---

## 현재 상태

| 항목 | 수치 | 비고 |
|---|---|---|
| 테이블 | **19** | CM 9 + CT 6 + BL 4 |
| 시퀀스 | 17 | VARCHAR PK 테이블 (CM_CODE_GROUP / CM_AUTH) 제외 |
| 인덱스 | 25 | PK/UNIQUE 외 보조 인덱스 |
| 코드 그룹 | 6 | EQUIPMENT_TYPE / CONTRACT_STATUS / BILLING_STATUS / PAYMENT_METHOD / VISIT_TYPE / NOTIFICATION_TYPE |
| 코드값 | 22 | |
| 역할 | 3 | SUPER_ADMIN / ADMIN / VIEWER |
| 메뉴 | 24 | GROUP 7 + LEAF 17 (2-depth 트리) |
| AUTH 키 | 59 | `{모듈}_{액션}` 명명 (ADR-008) |
| 역할-권한 매핑 | 111 | SUPER 59 + ADMIN 39 + VIEWER 13 |

ADR-008/009 권한 모델로 `CM_ROLE_MENU` 폐기 → `CM_AUTH` / `CM_ROLE_AUTH` / `CM_USER_AUTH` 신설.
사용자 (`CM_USER`) 는 AdminSeeder (ApplicationRunner) 가 첫 부팅 시 admin@rental.com / Admin1234! 자동 INSERT.

---

## 주의

- 본 디렉토리에 파일이 있고 `oracle-data` 볼륨이 비어 있을 때만 실행됨
- 이미 초기화된 볼륨이 있다면 새 SQL 추가해도 자동 실행 안 됨 → 수동 실행 또는 볼륨 제거 필요
- 볼륨 제거: `docker compose down -v` ⚠️ DB 데이터 전부 삭제됨

---

## 수동 실행 방법 (볼륨 유지하며 SQL 실행)

```bash
# 호스트에서 컨테이너로 sqlplus 진입
docker exec -it rental-oracle sqlplus rental/rental@//localhost:1521/XEPDB1

# 또는 SQL 파일 직접 실행
docker exec -i rental-oracle sqlplus rental/rental@//localhost:1521/XEPDB1 < your-script.sql
```

---

## fallback (start.bat)

`start.bat` 이 컨테이너 부팅 후 테이블 수 확인 (`SELECT COUNT(*) FROM USER_TABLES`):

| 결과 | 처리 |
|---|---|
| 19 | 정상 — fallback 스킵 |
| 0 | 자동 init 미실행 — 수동 fallback 실행 |
| 그 외 | 부분 실행 경고 — `reset.bat` (볼륨 삭제) 후 재시작 권장 |
