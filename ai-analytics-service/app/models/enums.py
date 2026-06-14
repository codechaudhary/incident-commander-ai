from enum import StrEnum


class TraceStatus(StrEnum):
    SUCCESS = "SUCCESS"
    ERROR = "ERROR"
    TIMEOUT = "TIMEOUT"


class AnalysisStatus(StrEnum):
    PENDING = "PENDING"
    PROCESSING = "PROCESSING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"


class FailureType(StrEnum):
    NONE = "NONE"
    SLOW_RESPONSE = "SLOW_RESPONSE"
    DB_TIMEOUT = "DB_TIMEOUT"
    RUNTIME_EXCEPTION = "RUNTIME_EXCEPTION"
