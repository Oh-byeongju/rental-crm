#!/bin/bash
# Kafka 토픽 초기 생성 스크립트
# docker-compose 의 kafka-init 서비스가 컨테이너 부팅 시 1회 실행.
#
# 토픽 명세: docs/04. 기능 명세서.md §0-3 (Kafka 이벤트 명세) 참조
# - rental.billing.created
# - rental.payment.completed
# - rental.payment.overdue
# - rental.visit.assigned

set -e

BROKER="kafka:29092"
PARTITIONS=3
REPLICATION=1

echo "[kafka-init] Waiting for Kafka broker..."
until kafka-topics --bootstrap-server "$BROKER" --list > /dev/null 2>&1; do
  sleep 2
done
echo "[kafka-init] Kafka broker is ready."

create_topic() {
  local TOPIC=$1
  if kafka-topics --bootstrap-server "$BROKER" --list | grep -qx "$TOPIC"; then
    echo "[kafka-init] Topic '$TOPIC' already exists. Skipping."
  else
    kafka-topics --bootstrap-server "$BROKER" \
      --create \
      --topic "$TOPIC" \
      --partitions "$PARTITIONS" \
      --replication-factor "$REPLICATION"
    echo "[kafka-init] Created topic: $TOPIC"
  fi
}

# rental-crm 도메인 토픽
create_topic "rental.billing.created"
create_topic "rental.payment.completed"
create_topic "rental.payment.overdue"
create_topic "rental.visit.assigned"

echo "[kafka-init] All topics ready."
kafka-topics --bootstrap-server "$BROKER" --list
