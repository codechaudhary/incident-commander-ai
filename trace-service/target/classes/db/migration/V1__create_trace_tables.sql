CREATE TABLE traces
(
    id UUID PRIMARY KEY,

    trace_id VARCHAR(64) NOT NULL UNIQUE,

    root_service VARCHAR(128) NOT NULL,

    root_operation VARCHAR(128) NOT NULL,

    status VARCHAR(16) NOT NULL,

    failure_type VARCHAR(32) NOT NULL,

    duration_ms BIGINT NOT NULL,

    started_at TIMESTAMP WITH TIME ZONE NOT NULL,

    ended_at TIMESTAMP WITH TIME ZONE NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_traces_trace_id
    ON traces(trace_id);

CREATE INDEX idx_traces_status
    ON traces(status);

CREATE INDEX idx_traces_started_at
    ON traces(started_at);



CREATE TABLE spans
(
    id UUID PRIMARY KEY,

    trace_id VARCHAR(64) NOT NULL,

    span_id VARCHAR(32) NOT NULL,

    parent_span_id VARCHAR(32),

    service_name VARCHAR(128) NOT NULL,

    operation VARCHAR(256) NOT NULL,

    status VARCHAR(16) NOT NULL,

    duration_ms BIGINT NOT NULL,

    started_at TIMESTAMP WITH TIME ZONE NOT NULL,

    ended_at TIMESTAMP WITH TIME ZONE NOT NULL,

    attributes JSONB NOT NULL,

    events JSONB NOT NULL,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_spans_trace
        FOREIGN KEY(trace_id)
        REFERENCES traces(trace_id)
);

CREATE INDEX idx_spans_trace_id
    ON spans(trace_id);

CREATE INDEX idx_spans_span_id
    ON spans(span_id);