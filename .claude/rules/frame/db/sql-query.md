# SQL 쿼리 룰

> 모든 SELECT/INSERT/UPDATE/DELETE 작성 시 적용. JPQL/Native Query/QueryDSL 무관 동일 적용.

## 1. SELECT 절

- **`SELECT *` 금지.** 필요한 컬럼만 명시한다.
- JPA Entity 전체 조회로 충분한 경우는 예외 — 다만 조회 전용이면 DTO Projection 우선 고려.
- 컬럼이 많으면 줄바꿈으로 가독성 확보.

## 2. 인덱스

- 모든 SELECT 는 **인덱스를 타게끔** 작성한다.
- 테이블 정의·인덱스 컬럼은 ERD 산출물(`docs/06_ERD 및 테이블 정의서.md`) 또는 DB 캐시(추후 도입 시) 를 참고.
- 비즈니스 로직 상 인덱스 생성이 필요하면 **사용자에게 요청** (Claude 가 임의로 추가하지 않음).

## 3. WHERE 절

- 인덱스 컬럼을 함수로 감싸지 않는다 (`WHERE TRUNC(col) = ...` 같은 패턴 회피).
- 묵시적 형 변환으로 인덱스가 무력화되지 않게 형을 명시.
- Oracle 의 경우 `DATE` ↔ `TIMESTAMP` 비교 주의.

## 4. JPA 추가 주의

- N+1 문제 방지 — 연관 엔티티 조회 시 `@EntityGraph` 또는 `fetch join` 사용 의식.
- 대량 처리 시 영속성 컨텍스트 부하 의식 — `flush` + `clear` 또는 JDBC bulk INSERT 검토.
