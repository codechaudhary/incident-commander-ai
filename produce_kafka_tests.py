import json
from kafka import KafkaProducer
import uuid
import time
from datetime import datetime, timezone

producer = KafkaProducer(
    bootstrap_servers=['localhost:9092'],
    value_serializer=lambda m: json.dumps(m).encode('utf-8'),
    key_serializer=lambda m: m.encode('utf-8')
)

topic = 'trace-ingested-topic'

def send_event(trace_id, status, failure_type, duration):
    payload = {
        "eventId": str(uuid.uuid4()),
        "eventType": "TRACE_INGESTED",
        "eventVersion": "1.0.0",
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "source": "trace-service",
        "payload": {
            "traceId": trace_id,
            "rootService": "checkout-service",
            "rootOperation": "POST /checkout",
            "status": status,
            "failureType": failure_type,
            "durationMs": duration,
            "startedAt": datetime.now(timezone.utc).isoformat(),
            "endedAt": datetime.now(timezone.utc).isoformat(),
            "spanCount": 5,
            "errorSpans": []
        }
    }
    producer.send(topic, key=trace_id, value=payload)
    print(f"Sent {status} / {failure_type} for trace {trace_id}")

# Scenario 1: CRITICAL via RUNTIME_EXCEPTION
send_event("trace-101", "ERROR", "RUNTIME_EXCEPTION", 120)

# Scenario 2: CRITICAL via DB_TIMEOUT > 5000ms
send_event("trace-102", "ERROR", "DB_TIMEOUT", 6000)

# Scenario 3: HIGH via DB_TIMEOUT < 5000ms
send_event("trace-103", "ERROR", "DB_TIMEOUT", 2000)

# Scenario 4: MEDIUM via SLOW_RESPONSE
send_event("trace-104", "ERROR", "SLOW_RESPONSE", 4000)

# Scenario 5: IGNORED via SUCCESS
send_event("trace-105", "SUCCESS", None, 150)

# Scenario 6: IGNORED via Duplicate traceId
send_event("trace-101", "ERROR", "RUNTIME_EXCEPTION", 120)

producer.flush()
print("All messages flushed to Kafka!")
