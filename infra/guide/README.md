# `infra/guide/` — 인프라 로컬 룰 + 운영 가이드

> Docker / Oracle / Kafka / Redis 인프라 작업 시 참고. Claude 가 자동 로드하지 않음.

---

## 빠른 시작

### 사전 요구사항
- **Docker Desktop** 실행 중 (Windows 작업표시줄에 고래 아이콘 확인)
- Docker 메모리 최소 4GB 권장 (Oracle 이 무거움)

### 딸깍 실행 (Windows)

`infra/` 폴더의 배치 파일을 **더블클릭**:

| 파일 | 동작 |
|---|---|
| **`start.bat`** | 인프라 시작 — `.env` 자동 생성 + Docker 검증 + `compose up -d` |
| **`stop.bat`** | 정지 (데이터 유지) |
| **`status.bat`** | 컨테이너/헬스/토픽 상태 한눈에 |
| **`reset.bat`** | ⚠️ 완전 초기화 — `YES` 입력 확인 필요. DB 데이터 삭제됨 |

### CLI 로 직접 실행 (PowerShell)
```powershell
cd infra
Copy-Item .env.example .env       # 최초 1회만
docker compose up -d              # 시작
docker compose ps                 # 상태
docker compose logs -f oracle     # Oracle 부팅 로그 (DATABASE IS READY TO USE! 대기)
docker compose stop               # 정지 (데이터 유지)
docker compose down               # 컨테이너 제거 (볼륨 유지)
docker compose down -v            # ⚠️ 볼륨까지 제거
```

### 접속 정보
| 서비스 | URL / 명령 |
|---|---|
| Oracle | `sqlplus rental/rental@//localhost:1521/XEPDB1` (또는 DataGrip/SQL Developer) |
| Kafka UI | http://localhost:8090 |
| Redis | `docker exec -it rental-redis redis-cli` → `PING` |

---

## 구성 요소

| 서비스 | 이미지 | 호스트 포트 | 용도 |
|---|---|---|---|
| `rental-oracle` | gvenzl/oracle-xe:21-slim-faststart | 1521 | Oracle 21c XE |
| `rental-zookeeper` | confluentinc/cp-zookeeper:7.5.0 | (내부) | Kafka 메타데이터 |
| `rental-kafka` | confluentinc/cp-kafka:7.5.0 | 9092 | 메시지 브로커 |
| `rental-kafka-init` | confluentinc/cp-kafka:7.5.0 | (one-shot) | 토픽 4개 생성 후 종료 |
| `rental-redis` | redis:7-alpine | 6379 | 캐시 / 세션 |
| `rental-kafka-ui` | provectuslabs/kafka-ui:latest | 8090 | Kafka 시각화 (학습용) |

**네트워크**: 모두 `rental-network` 브리지에 연결. 컨테이너 간 통신 시 `localhost` 가 아닌 컨테이너명 사용 (예: `oracle:1521`, `kafka:29092`).

**볼륨**: `rental-oracle-data`, `rental-redis-data` (named volume). Kafka 는 로그 일관성 위해 볼륨 미사용 (재시작 시 토픽 재생성).

---

## 트러블슈팅

### Oracle 가 자꾸 unhealthy 로 떨어진다
- 첫 부팅 5분 이상 소요. `start_period: 3m` 으로 잡혀 있지만 머신 성능에 따라 더 걸림.
- `docker compose logs -f oracle` 에서 `DATABASE IS READY TO USE!` 메시지가 보이면 정상.

### 1521 포트가 이미 사용 중
- 다른 Oracle 인스턴스 또는 사내 도구가 점유 중일 수 있음.
- `.env` 의 `ORACLE_PORT` 를 1522 등으로 변경.

### Kafka 토픽이 안 만들어짐
- `docker compose logs kafka-init` 확인.
- `create-topics.sh` 실행 권한 확인 (`chmod +x infra/init-scripts/kafka/create-topics.sh` — Windows 에서 Git 클론 시 자동 처리되지만 가끔 깨짐).

### `docker compose down -v` 실수로 실행
- DB 데이터 전부 삭제됨. 재기동하면 `init-scripts/oracle/` 의 SQL 다시 실행되며 빈 스키마 생성.
- 학습용이라 큰 손실 없음. 더미 데이터 재생성 필요.

### Windows 에서 healthcheck 스크립트 줄바꿈 오류
- `.gitattributes` 에 `*.sh text eol=lf` 설정되어 있어 자동 처리됨.
- 그래도 안 되면: `(Get-Content file.sh -Raw) -replace "\`r\`n", "\`n" | Set-Content file.sh -NoNewline`

---

## 로컬 룰 인덱스

> 현재 비어 있음. 인프라 작업 시작 후 첫 패턴이 정립되면 추출.

| 룰 | description | 파일 |
|---|---|---|
| (없음) | | |

### 후보 (작업 진입 시 추가될 가능성)
- Oracle DDL 작성 규칙 (시퀀스 명명·테이블스페이스·NLS_CHARACTERSET)
- Kafka 토픽 명명 / 파티션 / 보존 정책
- Redis 키 명명 / TTL 정책
- 환경별 docker-compose override (`docker-compose.override.yml`)
- Oracle 컨테이너 백업/복구 절차

---

## decisions/

infra 한정 ADR. 예: Oracle 21 XE 선택 근거, Kafka Confluent vs Apache, Redis maxmemory 정책 등.
전역 영향 결정은 [`docs/decisions/`](../../docs/decisions/) 에.

---

## 새 룰 추가 시

상세 절차: [`@.claude/rules/_meta/rule-management.md`](../../.claude/rules/_meta/rule-management.md)
