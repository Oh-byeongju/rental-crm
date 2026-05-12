"""
DB 스키마 캐시 갱신 - rental-crm (Oracle 21c XE Docker).

`docker exec rental-oracle sqlplus` 로 SQL 실행하여 cache txt 파일 갱신.
Claude 는 본 스크립트를 자동 실행하지 않음 — 사용자가 `/cache-refresh` 슬래시로 호출.

Usage:
    python "docs/cache/refresh.py"
    python "docs/cache/refresh.py" --verbose

전제:
    - Docker 컨테이너 `rental-oracle` 가 실행 중
    - 컨테이너 안에 sqlplus 가 있음 (gvenzl/oracle-xe 기본 포함)
    - DB 접속 정보는 본 파일 상단 상수 참조

갱신 시점:
    - DB 스키마 변경 시 (테이블/컬럼/인덱스 추가·변경)
    - 공통코드 시드 변경 시
"""

import os
import sys
import subprocess
from datetime import datetime

# ── 경로 ──
CACHE_DIR = os.path.abspath(os.path.dirname(__file__))
SQL_DIR = os.path.join(CACHE_DIR, "sql")

# ── DB 접속 정보 (infra/docker-compose.yml + application-local.yml 기준) ──
DOCKER_CONTAINER = "rental-oracle"
DB_USER = "rental"
DB_PASS = "rental"
DB_HOST = "localhost"
DB_PORT = "1521"
DB_SERVICE = "XEPDB1"

# ── SQL → TXT 매핑 ──
SQL_MAP = [
    ("table_select.sql", "table.txt"),
    ("code_select.sql",  "code.txt"),
]


def run_sql(sql_file: str, output_file: str, verbose: bool = False):
    """SQL 파일을 docker exec sqlplus 로 실행하고 결과를 txt 파일에 저장."""
    sql_path = os.path.join(SQL_DIR, sql_file)
    out_path = os.path.join(CACHE_DIR, output_file)

    if not os.path.exists(sql_path):
        return False, f"SQL 파일 없음: {sql_file}"

    with open(sql_path, encoding="utf-8") as f:
        sql_text = f.read()

    cmd = [
        "docker", "exec", "-i", DOCKER_CONTAINER,
        "sqlplus", "-s",
        f"{DB_USER}/{DB_PASS}@//{DB_HOST}:{DB_PORT}/{DB_SERVICE}",
    ]

    if verbose:
        print(f"  CMD: {' '.join(cmd)}")

    try:
        result = subprocess.run(
            cmd, input=sql_text, capture_output=True, text=True,
            timeout=60, encoding="utf-8", errors="replace",
        )
        if result.returncode != 0:
            err = result.stderr.strip()
            if not err:
                # sqlplus 는 ORA-/ERROR 를 stdout 으로 내보낼 때가 많음 → 끝부분에서 추출
                ora_lines = [ln.strip() for ln in result.stdout.splitlines()
                             if "ORA-" in ln or "ERROR at line" in ln]
                err = " / ".join(ora_lines[-3:]) if ora_lines else "(stderr/ORA-* 모두 없음)"
            return False, f"sqlplus 오류: {err[:200]}"

        # sqlplus 출력에 ERROR 가 섞여있을 수 있음 — 검증
        if "ORA-" in result.stdout:
            return False, f"SQL 오류 포함 (ORA-* 발견)"

        with open(out_path, "w", encoding="utf-8", newline="\n") as f:
            f.write(result.stdout)

        size = os.path.getsize(out_path) if os.path.exists(out_path) else 0
        if size == 0:
            return True, "WARNING: 0 bytes (빈 결과)"
        return True, f"OK ({size:,} bytes)"

    except FileNotFoundError:
        return False, "docker 명령어 없음. Docker Desktop 실행 + PATH 확인"
    except subprocess.TimeoutExpired:
        return False, "타임아웃 (60초 초과)"
    except Exception as e:
        return False, f"예외: {e}"


def run(verbose: bool = False) -> int:
    print(f"[cache-refresh] {datetime.now():%Y-%m-%d %H:%M:%S}")
    print(f"  DB: docker exec {DOCKER_CONTAINER} sqlplus "
          f"{DB_USER}@{DB_HOST}:{DB_PORT}/{DB_SERVICE}")
    print()

    results = []
    for sql_file, txt_file in SQL_MAP:
        ok, msg = run_sql(sql_file, txt_file, verbose)
        results.append((txt_file, ok, msg))

    # 결과 보고
    print("=" * 56)
    print(f"{'파일':<20} {'상태':<8} {'결과'}")
    print("-" * 56)
    for txt_file, ok, msg in results:
        status = "OK" if ok else "FAIL"
        print(f"  {txt_file:<18} {status:<8} {msg}")
    print("-" * 56)

    success = sum(1 for _, ok, _ in results if ok)
    fail    = len(results) - success
    print(f"  성공: {success}, 실패: {fail}")

    if fail > 0:
        print()
        print("[WARNING] 일부 캐시 갱신 실패. 기존 캐시 파일이 있다면 유지됨.")
        return 1

    print()
    print("[OK] 캐시 갱신 완료")
    return 0


if __name__ == "__main__":
    sys.exit(run(verbose="--verbose" in sys.argv))
