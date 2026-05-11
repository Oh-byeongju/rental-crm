# Oracle 초기화 스크립트 자리

> `gvenzl/oracle-xe` 이미지는 `/container-entrypoint-initdb.d/` 아래의 `.sql` / `.sh` 파일을
> **최초 컨테이너 부팅 시 1회 자동 실행** 한다. (볼륨이 비어 있을 때만)

---

## 실행 순서

알파벳/숫자 순서로 실행됨. 파일명 prefix 로 순서 제어:

```
01-create-schema.sql        ← 스키마/시퀀스/테이블 DDL
02-create-indexes.sql       ← 인덱스 생성
03-seed-codes.sql           ← 공통코드 초기값
04-seed-test-data.sql       ← 학습용 더미 데이터 (선택)
```

---

## 현재 상태

**비어 있음.** ERD 17개 테이블 컨펌 후 DDL 생성하여 본 디렉토리에 배치.

다음 일정 (ADR-001 기준):
- [ ] CT_* 블록 ERD 컨펌 (ADR-002)
- [ ] BL_* 블록 ERD 컨펌 (ADR-003)
- [ ] 17개 테이블 DDL 생성 → `01-create-schema.sql`
- [ ] 시퀀스 17개 DDL 생성 (`SEQ_<TABLE>`)
- [ ] 보조 인덱스 DDL → `02-create-indexes.sql`
- [ ] 공통코드 초기값 (`CM_CODE_GROUP`, `CM_CODE`) → `03-seed-codes.sql`

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
