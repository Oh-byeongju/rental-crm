# DB 스키마 캐시 갱신 작업지시서

## 0. 대상

`docs/cache/` 의 자동 영역 2종 텍스트 파일을 갱신:

- `table.txt` — 테이블 + 컬럼 + 제약조건 + 인덱스 + 시퀀스
- `code.txt` — 공통코드 / 역할 / AUTH / 메뉴 시드

갱신 스크립트: `docs/cache/refresh.py`

---

## Step 1: 스크립트 실행

```powershell
python "docs/cache/refresh.py"
```

- 인자 없음 — 2종 전체 갱신
- 스크립트가 `docker exec rental-oracle sqlplus` 호출하여 SQL 실행 후 각 `.txt` 파일에 결과 저장
- DB 접속 정보는 스크립트 상단 상수 (`rental/rental@//localhost:1521/XEPDB1`)

### 실패 처리

| 출력 | 의미 | 사용자 안내 |
|---|---|---|
| `[OK] 캐시 갱신 완료` | 정상 | 결과 표 그대로 보고 |
| `docker 명령어 없음` | Docker Desktop 미실행 | "Docker Desktop 실행 후 재시도" |
| `sqlplus 오류: ...` | DB 컨테이너 미실행 / 접속 실패 | "`docker ps` 로 `rental-oracle` 확인" |
| `타임아웃 (60초 초과)` | DB 응답 지연 | "Oracle 초기화 미완 (첫 부팅 3-5분) 또는 컨테이너 헬스 체크" |
| `ORA-xxxxx` 포함 | SQL 오류 | 오류 메시지 그대로 전달 |

---

## Step 2: 결과 리포트

스크립트가 출력하는 결과 표를 **그대로** 사용자에게 전달.

예시:
```
[cache-refresh] 2026-05-13 09:30:00
  DB: docker exec rental-oracle sqlplus rental@localhost:1521/XEPDB1

========================================================
파일                  상태       결과
--------------------------------------------------------
  table.txt          OK       12,345 bytes
  code.txt           OK       3,210 bytes
--------------------------------------------------------
  성공: 2, 실패: 0

[OK] 캐시 갱신 완료
```

추가 가공·해석 안 함. 스크립트 출력이 진실.

---

## 주의사항

1. **자동 영역 (2종 `.txt`) 만 갱신** — 다른 파일 건드리지 않음
2. **스크립트 자체를 수정하지 않음** — DB 접속 정보·SQL 경로는 사용자가 직접 관리
3. **갱신은 사용자 명시 호출 (`/cache-refresh`) 에만 발생** — Claude 자동 실행 금지 (룰: `docs/global-rules/db-cache-pattern.md`)
4. 스크립트 실행 실패 시 **수정 시도하지 않고** 사용자에게 원인 보고 후 종료
