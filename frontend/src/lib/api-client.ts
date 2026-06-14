import axios from "axios";
import { DashboardSummaryDto, IncidentDto, PagedResponse, TraceDto, AlertDto } from "@/types";

export const bffApi = axios.create({
  baseURL: "http://localhost:8080/api/v1",
});

export const simulatorApi = axios.create({
  baseURL: "http://localhost:8081/api/v1",
});

export const getDashboardSummary = async (): Promise<DashboardSummaryDto> => {
  const { data } = await bffApi.get<DashboardSummaryDto>("/dashboard/summary");
  return data;
};

export const getIncidentDetail = async (traceId: string): Promise<IncidentDto> => {
  const { data } = await bffApi.get<IncidentDto>(`/incidents/${traceId}`);
  return data;
};

export const getTraces = async (page = 0, size = 20, status?: string): Promise<PagedResponse<TraceDto>> => {
  const url = status 
    ? `/traces?page=${page}&size=${size}&status=${status}` 
    : `/traces?page=${page}&size=${size}`;
  const { data } = await bffApi.get<PagedResponse<TraceDto>>(url);
  return data;
};

export const getAlerts = async (status?: string, size = 100): Promise<PagedResponse<AlertDto>> => {
  const url = status 
    ? `/alerts?status=${status}&size=${size}` 
    : `/alerts?size=${size}`;
  const { data } = await bffApi.get<PagedResponse<AlertDto>>(url);
  return data;
};

export const acknowledgeAlert = async (alertId: string): Promise<void> => {
  await bffApi.patch(`/alerts/${alertId}/status`, { status: "ACKNOWLEDGED" });
};
