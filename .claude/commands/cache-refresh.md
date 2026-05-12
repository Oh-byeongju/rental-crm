# DB 스키마 캐시 갱신

`docs/cache/` 의 자동 영역 (table.txt / code.txt) 을 일괄 갱신한다.
**스크립트를 수정하지 않는다. 결과만 사용자에게 보고한다.**

## 실행

`docs/commands/cache-refresh-spec.md` 의 절차를
**처음부터 끝까지 그대로** 수행한다.
해석하지 말고 작업지시서에 적힌 그대로 실행한다.

## 인자

없음 — 2종 (table.txt / code.txt) 전체 갱신.

## 순서

1. 작업지시서 읽기
2. Step 1: `python "docs/cache/refresh.py"` 실행
3. Step 2: 스크립트 출력을 그대로 사용자에게 전달
4. 실패 시 원인 보고 (수정 시도 X)
