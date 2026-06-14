from __future__ import annotations
from functools import lru_cache
from typing import Literal

from pydantic import Field, PostgresDsn, RedisDsn, computed_field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = "ai-analysis-service"
    environment: str = "local"
    log_level: str = "INFO"
    api_prefix: str = "/api/v1"

    host: str = "0.0.0.0"
    port: int = 8090

    storage_backend: Literal["json", "postgresql", "redis"] = "json"
    json_database_path: str = "data/ai_analysis.json"

    database_url: PostgresDsn = Field(
        default="postgresql://incidents:incidents@localhost:5432/incidents"
    )

    kafka_bootstrap_servers: str = "kafka:9092"
    kafka_trace_topic: str = "trace-events"
    kafka_consumer_group: str = "ai-analysis-group"
    kafka_consumer_enabled: bool = True

    redis_url: RedisDsn = "redis://redis:6379"
    redis_analysis_channel: str = "analysis:live"
    redis_publish_enabled: bool = False

    llm_provider: Literal["opencode", "stub"] = "stub"
    opencode_api_key: str | None = None
    opencode_base_url: str = "https://opencode.ai/zen/v1"
    opencode_models: str = (
        "deepseek-v4-flash-free,qwen3.6-plus-free,"
        "minimax-m3-free,nemotron-3-ultra-free"
    )
    llm_max_tokens: int = 4500
    llm_temperature: float = 0.2
    llm_timeout_seconds: float = 180.0

    @computed_field  # type: ignore[prop-decorator]
    @property
    def async_database_url(self) -> str:
        raw = str(self.database_url)
        if raw.startswith("postgresql+asyncpg://"):
            return raw
        return raw.replace("postgresql://", "postgresql+asyncpg://", 1)

    @property
    def is_production(self) -> bool:
        return self.environment.lower() in {"prod", "production"}

    @property
    def llm_models(self) -> tuple[str, ...]:
        models = tuple(model.strip() for model in self.opencode_models.split(",") if model.strip())
        if not models:
            raise ValueError("OPENCODE_MODELS must contain at least one model")
        return models


@lru_cache
def get_settings() -> Settings:
    return Settings()
