from __future__ import annotations
from enum import Enum


class TraceStatus(str, Enum):
    SUCCESS = "SUCCESS"
    ERROR = "ERROR"
    TIMEOUT = "TIMEOUT"


class AnalysisStatus(str, Enum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


class FailureType(str, Enum):
    NONE = "NONE"
    SLOW_RESPONSE = "SLOW_RESPONSE"
    DB_TIMEOUT = "DB_TIMEOUT"
    RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION"
