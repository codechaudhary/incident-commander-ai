from datetime import UTC, datetime

from fastapi import Request
from fastapi.responses import JSONResponse
from starlette import status


class NotFoundError(Exception):
    def __init__(self, message: str) -> None:
        self.message = message


def error_payload(
    *,
    request: Request,
    http_status: int,
    error: str,
    message: str,
) -> dict[str, object]:
    trace_id = request.headers.get("traceparent")
    return {
        "timestamp": datetime.now(UTC).isoformat().replace("+00:00", "Z"),
        "status": http_status,
        "error": error,
        "message": message,
        "path": request.url.path,
        "traceId": trace_id,
    }


async def not_found_handler(request: Request, exc: NotFoundError) -> JSONResponse:
    return JSONResponse(
        status_code=status.HTTP_404_NOT_FOUND,
        content=error_payload(
            request=request,
            http_status=status.HTTP_404_NOT_FOUND,
            error="Not Found",
            message=exc.message,
        ),
    )


async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content=error_payload(
            request=request,
            http_status=status.HTTP_500_INTERNAL_SERVER_ERROR,
            error="Internal Server Error",
            message="Unexpected service error",
        ),
    )
