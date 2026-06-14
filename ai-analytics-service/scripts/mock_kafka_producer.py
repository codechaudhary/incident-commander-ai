import asyncio
import json
from datetime import UTC, datetime, timedelta
from uuid import uuid4

from aiokafka import AIOKafkaProducer

BOOTSTRAP_SERVERS = "localhost:9092"
TOPIC = "trace-events"


async def main() -> None:
    trace_id = "4bf92f3577b34da6a3ce929d0e0e4736"
    now = datetime.now(UTC)
    event = {
        "eventId": str(uuid4()),
        "eventType": "TRACE_INGESTED",
        "eventVersion": "1.0.0",
        "timestamp": now.isoformat().replace("+00:00", "Z"),
        "source": "trace-service",
        "payload": {
            "traceId": trace_id,
            "rootService": "order-service",
            "rootOperation": "POST /orders",
            "status": "ERROR",
            "failureType": "DB_TIMEOUT",
            "durationMs": 5200,
            "startedAt": now.isoformat().replace("+00:00", "Z"),
            "endedAt": (now + timedelta(milliseconds=5200)).isoformat().replace("+00:00", "Z"),
            "spanCount": 4,
            "errorSpans": [
                {
                    "spanId": "a2fb4a1d1a96d312",
                    "serviceName": "payment-service",
                    "operation": "POST /charge",
                    "errorMessage": "RuntimeException: DB timeout after 5000ms",
                    "durationMs": 5100,
                }
            ],
        },
    }
    producer = AIOKafkaProducer(
        bootstrap_servers=BOOTSTRAP_SERVERS,
        value_serializer=lambda value: json.dumps(value).encode("utf-8"),
        key_serializer=lambda value: value.encode("utf-8"),
    )
    await producer.start()
    try:
        await producer.send_and_wait(TOPIC, event, key=trace_id)
    finally:
        await producer.stop()


if __name__ == "__main__":
    asyncio.run(main())
