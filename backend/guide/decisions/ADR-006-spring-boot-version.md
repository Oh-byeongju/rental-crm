# ADR-006 (Backend Local) — Spring Boot 3.5.0 + Java 21 채택

- **작성일**: 2026-05-11
- **상태**: 확정

---

## 1. Context

Spring Initializr 디폴트 버전이 **4.0.6** 으로 잡힘.
호환 범위: `>=3.5.0`. 즉 3.4 이하는 더 이상 신규 생성 불가.

선택지:
- A: Spring Boot **3.5.0** — 3.x 시리즈 최신 안정
- B: Spring Boot **4.0.6** — 최신 GA, 디폴트

---

## 2. Decision — **3.5.0 채택**

근거:
- 학습 프로젝트의 본 목표는 경험 피드백 보완 (bulk INSERT, Kafka, 쿼리 튜닝) — Spring Boot 버전과 무관
- 4.0.x 는 일부 starter 이름이 변경됨 (예: `spring-boot-starter-webmvc`, `spring-boot-starter-actuator-test` 분리) — 자료 부족
- 리뷰어가 대부분 3.x 환경에 익숙
- 3.x 자료 / Stack Overflow / Baeldung 자료 압도적으로 풍부
- 학습 곡선 가파른 부분(JPA, Kafka, Spring Security) 에 집중 — 프레임워크 버전 추격에 시간 소모 방지

비채택(4.0.6) 사유:
- starter 의존성 이름 변경으로 자료 검색 어려움
- 학습 자산 측면에서 단기 손해. 신기술은 회사 입사 후 자연 습득

---

## 3. Consequences

### 긍정
- 안정성 + 풍부한 자료
- 코드 리뷰 시 별다른 해명 불필요

### 부정
- "최신 미사용" 비판 가능 — 단, "안정 LTS 채택" 으로 답변 가능
- Spring Boot 4 의 변경 사항(가상 스레드 기본화 등) 미체험

---

## 4. Java 21 (LTS) 채택

### 결정
- 본 프로젝트는 **Java 21 LTS** 채택. Spring Boot 3.5.0 호환 (3.x 는 Java 17~21 지원).
- 시스템 `JAVA_HOME` 은 **17 그대로 유지** (다른 프로젝트 호환성 보호).
- 본 프로젝트만 21 사용 — `backend/gradle.properties` 의
  `org.gradle.java.installations.paths` 가 `C:\WORK\JDK\openjdk-21+35_windows-x64_bin` 지정.

### 근거
- Java 21 LTS 출시 (2023-09-19), 안정성 검증됨
- **Virtual Threads** (Project Loom) — 학습 가치 큼 (Spring Boot 3.x 가상 스레드 지원)
- **Pattern Matching for switch**, **Record Patterns**, **Sequenced Collections**
- 포트폴리오에서 "최신 기술 따라간다" 어필 가능
- 시스템 JDK 안 건드려도 됨 — Gradle Toolchain 으로 격리

### 검증 명령
```powershell
cd backend
.\gradlew -v        # JVM: 21 (시스템 JAVA_HOME 17 아님) 확인
.\gradlew build     # 컴파일 + 의존성 다운로드
```

---

## 5. 추후 검토

- Spring Boot 4.x 가 자료 풍부해지는 시점에 마이그레이션 (대략 2026 후반)
- 마이그레이션 가이드: starter 이름 변경 + Java 21 의 신기능 본격 활용
