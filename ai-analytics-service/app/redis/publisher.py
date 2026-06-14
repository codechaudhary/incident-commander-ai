import json

import redis.asyncio as redis

from app.core.config import Settings
from app.models.schemas import AnalysisDto


class AnalysisPublisher:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._client = redis.from_url(str(settings.redis_url), decode_responses=True)

    async def publish_completed(self, analysis: AnalysisDto) -> None:
        message = {
            "type": "ANALYSIS_COMPLETED",
            "analysisId": analysis.analysis_id,
            "traceId": analysis.trace_id,
            "alertId": analysis.alert_id,
            "status": analysis.status,
            "rootCause": analysis.root_cause,
            "recommendations": analysis.recommendations,
            "confidenceScore": analysis.confidence_score,
            "completedAt": analysis.completed_at.isoformat() if analysis.completed_at else None,
        }
        await self._client.publish(self.settings.redis_analysis_channel, json.dumps(message))

    async def ping(self) -> bool:
        return bool(await self._client.ping())

    async def close(self) -> None:
        await self._client.aclose()
