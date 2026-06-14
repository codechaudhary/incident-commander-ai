CREATE TABLE alerts (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alert_id     VARCHAR(64)  NOT NULL UNIQUE,
    trace_id     VARCHAR(64)  NOT NULL,
    severity     VARCHAR(16)  NOT NULL,
    status       VARCHAR(24)  NOT NULL DEFAULT 'OPEN',
    title        VARCHAR(256) NOT NULL,
    description  TEXT         NOT NULL,
    triggered_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alerts_alert_id     ON alerts(alert_id);
CREATE INDEX idx_alerts_trace_id     ON alerts(trace_id);
CREATE INDEX idx_alerts_status       ON alerts(status);
CREATE INDEX idx_alerts_severity     ON alerts(severity);
CREATE INDEX idx_alerts_triggered_at ON alerts(triggered_at DESC);
