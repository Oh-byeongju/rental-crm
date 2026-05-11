# 메서드 순서 — CRUD

> Service / Repository / Controller 의 메서드 정의 순서 컨벤션.

## 규칙

CRUD 순서로 작성한다:

1. **C**reate — `save*`, `create*`, `register*` (신규 등록)
2. **R**ead — `find*`, `get*`, `select*`
3. **U**pdate — `update*`, `modify*`
4. **D**elete — `delete*`, `remove*`

특수 메서드(승인 / 취소 / 콜백 / 외부 시스템 연동 / 배치 등) 는 CRUD 뒤에 별도 섹션으로 묶는다.

## JPA Repository 주의

`JpaRepository.save()` 는 신규 INSERT 와 UPDATE 를 모두 처리한다. 의도가 분명하도록 호출 측 메서드명에서 구분:

- 신규 등록 의도 → `register*`, `create*` 로 감싸는 Service 메서드 안에서 호출
- 수정 의도 → `update*` 로 감싸는 Service 메서드 안에서 호출

Repository 인터페이스 자체에는 `save` 가 한 번 있으면 충분 — 별도 메서드 추가 금지.

## 목적

가독성·일관성. 어느 클래스를 열어도 같은 순서로 메서드를 찾을 수 있게 한다.
