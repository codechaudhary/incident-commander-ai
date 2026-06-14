export type TraceStatus   = "SUCCESS" | "ERROR" | "TIMEOUT";
export type SpanStatus    = "OK" | "ERROR";
export type AlertSeverity = "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
export type AlertStatus   = "OPEN" | "ACKNOWLEDGED" | "RESOLVED";
export type AnalysisStatus = "PENDING" | "PROCESSING" | "COMPLETED" | "FAILED";
export type FailureType   = "NONE" | "SLOW_RESPONSE" | "DB_TIMEOUT" | "RUNTIME_EXCEPTION";

export interface SpanEvent {
  timeUnixNano: string;
  name: string;
  attributes: Record<string, unknown>;
}

export interface SpanDto {
  spanId: string;
  parentSpanId: string | null;
  serviceName: string;
  operation: string;
  status: SpanStatus;
  durationMs: number;
  startedAt: string;
  endedAt: string;
  attributes: Record<string, unknown>;
  events: SpanEvent[];
}

export interface TraceDto {
  id: string;
  traceId: string;
  rootService: string;
  rootOperation: string;
  status: TraceStatus;
  failureType: FailureType;
  durationMs: number;
  startedAt: string;
  endedAt: string;
  spanCount: number;
  createdAt: string;
  spans?: SpanDto[];
}

export interface AlertDto {
  id: string;
  alertId: string;
  traceId: string;
  severity: AlertSeverity;
  status: AlertStatus;
  title: string;
  description: string;
  triggeredAt: string;
  updatedAt: string;
}

export interface AnalysisDto {
  id: string;
  analysisId: string;
  traceId: string;
  alertId: string | null;
  status: AnalysisStatus;
  rootCause: string | null;
  affectedServices: string[];
  recommendations: string[];
  confidenceScore: number | null;
  modelUsed: string | null;
  latencyMs?: number | null;
  createdAt: string;
  completedAt: string | null;
}

export interface IncidentDto {
  trace: TraceDto;
  alert: AlertDto | null;
  analysis: AnalysisDto | null;
}

export interface DashboardSummaryDto {
  totalTraces: number;
  errorTraces: number;
  openAlerts: number;
  criticalAlerts: number;
  completedAnalyses: number;
  avgTraceDurationMs: number;
  lastUpdated: string;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface WsAlertMessage {
  type: "ALERT_CREATED";
  alertId: string;
  traceId: string;
  severity: AlertSeverity;
  status: AlertStatus;
  title: string;
  description: string;
  triggeredAt: string;
}

export interface WsAnalysisMessage {
  type: "ANALYSIS_COMPLETED";
  analysisId: string;
  traceId: string;
  alertId: string | null;
  status: AnalysisStatus;
  rootCause: string;
  recommendations: string[];
  confidenceScore: number;
  latencyMs?: number | null;
  completedAt: string;
}

export interface WsTraceMessage {
  type: "TRACE_INGESTED";
  traceId: string;
  rootService: string;
  rootOperation: string;
  status: TraceStatus;
  failureType: FailureType;
  durationMs: number;
  startedAt: string;
}
