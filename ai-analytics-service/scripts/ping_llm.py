# ruff: noqa: E402,I001
import argparse
import asyncio
import os
import sys
from dataclasses import dataclass
from pathlib import Path

import httpx


ROOT_DIR = Path(__file__).resolve().parents[1]
DEFAULT_MODELS = (
    "deepseek-v4-flash-free,qwen3.6-plus-free,minimax-m3-free,nemotron-3-ultra-free"
)


@dataclass(frozen=True)
class LLMSettings:
    api_key: str | None
    base_url: str
    models: tuple[str, ...]
    max_tokens: int
    temperature: float
    timeout_seconds: float


def load_dotenv(path: Path) -> None:
    if not path.exists():
        return

    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip().strip('"').strip("'")
        os.environ.setdefault(key, value)


def env_int(name: str, default: int) -> int:
    value = os.getenv(name)
    return int(value) if value else default


def env_float(name: str, default: float) -> float:
    value = os.getenv(name)
    return float(value) if value else default


def load_settings() -> LLMSettings:
    load_dotenv(ROOT_DIR / ".env")
    raw_models = os.getenv("OPENCODE_MODELS", DEFAULT_MODELS)
    models = tuple(model.strip() for model in raw_models.split(",") if model.strip())
    return LLMSettings(
        api_key=os.getenv("OPENCODE_API_KEY"),
        base_url=os.getenv("OPENCODE_BASE_URL", "https://opencode.ai/zen/v1"),
        models=models,
        max_tokens=env_int("LLM_MAX_TOKENS", 4500),
        temperature=env_float("LLM_TEMPERATURE", 0.2),
        timeout_seconds=env_float("LLM_TIMEOUT_SECONDS", 180.0),
    )


async def ping_model(settings: LLMSettings, model: str, message: str) -> tuple[str, dict]:
    async with httpx.AsyncClient(
        base_url=settings.base_url.rstrip("/") + "/",
        timeout=settings.timeout_seconds,
    ) as client:
        response = await client.post(
            "chat/completions",
            headers={"Authorization": f"Bearer {settings.api_key}"},
            json={
                "model": model,
                "temperature": settings.temperature,
                "max_tokens": settings.max_tokens,
                "messages": [
                    {"role": "system", "content": "You are a helpful assistant."},
                    {"role": "user", "content": message},
                ],
            },
        )
        response.raise_for_status()
        body = response.json()
        return body["choices"][0]["message"]["content"], body.get("usage") or {}


async def run(args: argparse.Namespace) -> int:
    settings = load_settings()
    if not settings.api_key:
        print("OPENCODE_API_KEY is required in .env.", file=sys.stderr)
        return 2

    last_error: Exception | None = None
    for model in settings.models:
        print(f"Trying model: {model}")
        try:
            content, usage = await ping_model(settings, model, args.message)
        except Exception as exc:
            last_error = exc
            print(f"Failed: {type(exc).__name__}: {exc}")
            continue

        print("\nLLM responded successfully.")
        print(f"Model used: {model}")
        print(f"Prompt tokens: {usage.get('prompt_tokens')}")
        print(f"Completion tokens: {usage.get('completion_tokens')}")
        print("\nResponse:")
        print(content)
        return 0

    print(f"All configured models failed. Last error: {last_error}", file=sys.stderr)
    return 1


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Send a simple chat message to OpenCode LLMs.")
    parser.add_argument(
        "message",
        nargs="?",
        default="hi tell me about rag",
        help="Message to send to the model. Defaults to 'hi'.",
    )
    return parser.parse_args()


if __name__ == "__main__":
    raise SystemExit(asyncio.run(run(parse_args())))
