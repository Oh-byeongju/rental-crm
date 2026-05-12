# DB 스키마 캐시

> Oracle 21c XE 의 schema + 공통 시드 데이터를 텍스트 파일로 캐싱.
> Claude 가 매번 ERD/시드 문서를 읽는 대신 본 캐시를 빠르게 grep — context 토큰 절감.

---

## 디렉토리

```
docs/cache/
├── refresh.py             ← Python wrapper (docker exec sqlplus)
├── sql/
│   ├── table_select.sql   ← 테이블 + 컬럼 + 제약 + 인덱스 + 시퀀스
│   └── code_select.sql    ← 공통코드 + 역할 + AUTH + 메뉴 시드
├── table.txt              ← (자동 생성) table_select.sql 결과
├── code.txt               ← (자동 생성) code_select.sql 결과
└── README.md
```

---

## 사용

### 갱신
```powershell
python "docs/cache/refresh.py"
python "docs/cache/refresh.py" --verbose
```

또는 슬래시 커맨드:
```
/cache-refresh
```

### 갱신 시점
- DB 스키마 변경 후 (DDL 추가·수정)
- 공통코드 / 역할 / AUTH / 메뉴 시드 변경 후
- 캐시 파일이 옛 정보 같을 때

### Claude 가 본 캐시를 사용하는 시점
[docs/global-rules/db-cache-pattern.md](../global-rules/db-cache-pattern.md) 참조.

---

## 출력 형식

파이프(`|`) 구분 텍스트. 섹션은 `=== {SECTION_NAME} ===` 헤더로 구분.

### table.txt 섹션
```
=== TABLES ===
TABLE_NAME|COMMENTS
...

=== COLUMNS ===
TABLE_NAME|COLUMN_NAME|DATA_TYPE|DATA_LENGTH|NULLABLE
...

=== CONSTRAINTS ===
TABLE_NAME|CONSTRAINT_NAME|TYPE|COLUMNS|REFERENCES
...

=== INDEXES ===
TABLE_NAME|INDEX_NAME|UNIQUENESS|COLUMNS
...

=== SEQUENCES ===
SEQUENCE_NAME|LAST_NUMBER|INCREMENT_BY
...
```

### code.txt 섹션
```
=== CODE GROUPS ===
=== CODES ===
=== ROLES ===
=== AUTHS ===
=== MENUS ===
```

---

## 주의

- 본 스크립트는 **DB 를 변경하지 않음**. 조회만.
- DB 컨테이너 (`rental-oracle`) 가 실행 중이어야 함. 아니면 `[FAIL] docker 명령어 없음` 또는 `sqlplus 오류`.
- 캐시 파일은 git 추적 — 갱신 후 commit.
- 본 스크립트는 Oracle 전용. PostgreSQL 등으로 옮기면 SQL 파일 + DB 접속 부분 수정.

---

> 패턴 출처: 참고 프로젝트 `guide/01. references/01. db/cache/` (PostgreSQL — rental-crm 도메인에 맞춰 Oracle 로 변형).
