import asyncio
import json
from contextlib import suppress

import structlog
from aiokafka import AIOKafkaConsumer
from pydantic import ValidationError

from app.core.config import Settings
from app.models.schemas import KafkaEnvelope
from app.services.analysis_service import AnalysisService

logger = structlog.get_logger(__name__)


class TraceEventConsumer:
    def __init__(self, settings: Settings, analysis_service: AnalysisService) -> None:
        self.settings = settings
        self.analysis_service = analysis_service
        self._consumer: AIOKafkaConsumer | None = None
        self._task: asyncio.Task[None] | None = None
        self._stop_event = asyncio.Event()

    async def start(self) -> None:
        self._consumer = AIOKafkaConsumer(
            self.settings.kafka_trace_topic,
            bootstrap_servers=self.settings.kafka_bootstrap_servers,
            group_id=self.settings.kafka_consumer_group,
            enable_auto_commit=False,
            auto_offset_reset="earliest",
            value_deserializer=lambda value: json.loads(value.decode("utf-8")),
        )
        await self._consumer.start()
        self._task = asyncio.create_task(self._consume_loop())
        logger.info("kafka_consumer_started", topic=self.settings.kafka_trace_topic)

    async def stop(self) -> None:
        self._stop_event.set()
        if self._task:
            self._task.cancel()
            with suppress(asyncio.CancelledError):
                await self._task
        if self._consumer:
            await self._consumer.stop()
        logger.info("kafka_consumer_stopped")

    async def _consume_loop(self) -> None:
        if self._consumer is None:
            raise RuntimeError("Kafka consumer is not started")

        while not self._stop_event.is_set():
            try:
                async for message in self._consumer:
                    await self._handle_message(message.value)
                    await self._consumer.commit()
                    if self._stop_event.is_set():
                        break
            except asyncio.CancelledError:
                raise
            except Exception as exc:
                logger.exception("kafka_consume_error", error=str(exc))
                await asyncio.sleep(2)

    async def _handle_message(self, raw_message: dict[str, object]) -> None:
        try:
            envelope = KafkaEnvelope.model_validate(raw_message)
        except ValidationError as exc:
            logger.warning("trace_event_invalid", error=str(exc), payload=raw_message)
            return

        if envelope.event_type != "TRACE_INGESTED":
            logger.info("trace_event_ignored", event_type=envelope.event_type)
            return

        await self.analysis_service.process_trace_event(envelope.payload)
