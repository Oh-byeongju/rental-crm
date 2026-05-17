---
description: "DB 스키마/시드 조회 시 — docs/cache/ 텍스트 캐시 우선 (DB 직접 조회 금지)"
---

# DB 캐시 사용 패턴

## 적용 시점

Claude 가 다음 정보를 필요로 할 때:

- 테이블 이름 · 컬럼 목록 · 데이터 타입 · NULLABLE
- 인덱스 목록 + 인덱스 컬럼
- PK / UNIQUE / FK / CHECK 제약조건
- 시퀀스 현재값
- 공통코드 그룹 / 코드값 시드
- 역할 / AUTH / 메뉴 시드

## 우선순위

1. **`docs/cache/{table,code}.txt` 부터 grep** — 가장 빠름. context 토큰 절감.
2. 캐시에 없는 정보 → ERD 문서 (`docs/06_ERD 및 테이블 정의서.md`) Read
3. ERD 에도 없는 디테일 → DDL 파일 (`infra/init-scripts/oracle/01-create-schema.sql`) Read
4. **DB 에 직접 조회 / `refresh.py` 자동 실행 금지** — 사용자 명시 호출 (`/cache-refresh`) 만 캐시 갱신

## 캐시가 옛 정보 같으면

- 사용자에게 안내: "캐시가 옛 정보일 수 있음. `/cache-refresh` 실행 권장"
- Claude 가 직접 `python refresh.py` 호출 **안 함**

## 캐시 파일이 없으면

첫 사용자가 `python docs/cache/refresh.py` 또는 `/cache-refresh` 한 번 실행.
생성된 `.txt` 는 git commit (다른 사용자/노트북도 즉시 사용).

## 왜 캐시 패턴인가

| 방식 | 속도 | Context 비용 | 정확성 |
|---|---|---|---|
| DB 직접 조회 (Bash docker exec) | 느림 | 높음 | 실시간 ✅ |
| ERD 문서 Read | 중간 | 중간 (큰 마크다운) | 사람 갱신 시점 |
| **캐시 텍스트 grep** | **빠름** | **낮음** | refresh 시점 |

일반 작업에 캐시 우선. *정확성이 critical* 한 경우만 ERD/DDL 직접 Read.

## 갱신 책임 분담

| | Claude | 사용자 |
|---|---|---|
| DB 스키마 변경 (DDL 추가) | 안내만 | 직접 SQL 실행 |
| 캐시 파일 갱신 | 안내만 (`/cache-refresh` 권장) | 슬래시 호출 |
| 갱신된 캐시 commit | 작업 진행 도움 | 의사결정 |

→ Claude 의 역할: *사용 + 안내*. 갱신·DB 변경은 사용자.
