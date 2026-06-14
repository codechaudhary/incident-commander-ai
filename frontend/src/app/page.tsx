"use client";

import { useEffect, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { motion, AnimatePresence } from "framer-motion";
import { containerVariants, itemVariants } from "@/lib/animations";
import { getDashboardSummary, getAlerts, getTraces } from "@/lib/api-client";
import { getStompClient } from "@/lib/ws";
import { WsAlertMessage, WsTraceMessage, AlertDto, TraceDto } from "@/types";
import { Card } from "@/components/Card";
import { Button } from "@/components/Button";
import Link from "next/link";

export default function Dashboard() {
  const [liveAlerts, setLiveAlerts] = useState<AlertDto[]>([]);
  const [liveTraces, setLiveTraces] = useState<TraceDto[]>([]);

  const { data: summary } = useQuery({
    queryKey: ["dashboardSummary"],
    queryFn: getDashboardSummary,
    refetchInterval: 10000,
  });

  const { data: initialAlerts } = useQuery({
    queryKey: ["initialAlerts"],
    queryFn: () => getAlerts("OPEN", 5),
  });

  const { data: initialTraces } = useQuery({
    queryKey: ["initialTraces"],
    queryFn: () => getTraces(0, 5),
  });

  useEffect(() => {
    if (initialAlerts) setLiveAlerts(initialAlerts.content);
    if (initialTraces) setLiveTraces(initialTraces.content);
  }, [initialAlerts, initialTraces]);

  useEffect(() => {
    const client = getStompClient();
    
    const onConnect = () => {
      client.subscribe("/topic/alerts", (message) => {
        const alertMsg = JSON.parse(message.body) as WsAlertMessage;
        setLiveAlerts((prev) => {
          const newAlert: AlertDto = {
            id: alertMsg.alertId,
            alertId: alertMsg.alertId,
            traceId: alertMsg.traceId,
            severity: alertMsg.severity,
            status: alertMsg.status,
            title: alertMsg.title,
            description: alertMsg.description,
            triggeredAt: alertMsg.triggeredAt,
            updatedAt: alertMsg.triggeredAt,
          };
          return [newAlert, ...prev].slice(0, 5);
        });
      });

      client.subscribe("/topic/traces", (message) => {
        const traceMsg = JSON.parse(message.body) as WsTraceMessage;
        setLiveTraces((prev) => {
          const newTrace: TraceDto = {
            id: traceMsg.traceId,
            traceId: traceMsg.traceId,
            rootService: traceMsg.rootService,
            rootOperation: traceMsg.rootOperation,
            status: traceMsg.status,
            failureType: traceMsg.failureType,
            durationMs: traceMsg.durationMs,
            startedAt: traceMsg.startedAt,
            endedAt: traceMsg.startedAt,
            spanCount: 1,
            createdAt: traceMsg.startedAt,
          };
          return [newTrace, ...prev].slice(0, 5);
        });
      });
    };

    if (client.connected) {
      onConnect();
    } else {
      client.onConnect = onConnect;
    }

    return () => {
      if (client.connected) {
        client.unsubscribe("/topic/alerts");
        client.unsubscribe("/topic/traces");
      }
    };
  }, []);

  return (
    <motion.div
      initial="hidden"
      animate="visible"
      exit="exit"
      variants={containerVariants}
      className="max-w-6xl w-full mx-auto p-6 md:p-12"
    >
      <motion.header variants={itemVariants} className="flex flex-col md:flex-row md:items-end justify-between mb-12 gap-6 border-b border-border-color pb-8">
        <div>
          <h1 className="font-mono text-4xl font-bold text-foreground tracking-wider uppercase mb-2">
            Incident Management
          </h1>
          <p className="font-sans text-secondary-text text-lg">
            Real-time production incident response.
          </p>
        </div>
        <div className="flex gap-4">
          <Link href="/simulate">
            <Button variant="primary">Simulate Outage</Button>
          </Link>
        </div>
      </motion.header>

      <motion.section variants={containerVariants} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
        <Card title="Total Traces" className="mb-0">
          <p className="text-4xl font-bold font-mono">
            {summary ? summary.totalTraces : <span className="text-secondary-text/50">...</span>}
          </p>
        </Card>
        <Card title="Error Rate" className="mb-0">
          <p className="text-4xl font-bold font-mono text-accent-red">
            {summary ? (summary.totalTraces > 0 ? ((summary.errorTraces / summary.totalTraces) * 100).toFixed(1) + "%" : "0%") : <span className="text-secondary-text/50">...</span>}
          </p>
        </Card>
        <Card title="Open Alerts" className="mb-0">
          <p className="text-4xl font-bold font-mono text-warning">
            {summary ? summary.openAlerts : <span className="text-secondary-text/50">...</span>}
          </p>
        </Card>
        <Card title="AI Analyses" className="mb-0">
          <p className="text-4xl font-bold font-mono text-primary">
            {summary ? summary.completedAnalyses : <span className="text-secondary-text/50">...</span>}
          </p>
        </Card>
      </motion.section>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-12">
        <motion.section variants={containerVariants}>
          <h2 className="font-mono text-xl font-bold tracking-wider uppercase mb-6 border-b border-border-color pb-2">
            Live Alerts
          </h2>
          <div className="flex flex-col gap-4">
            <AnimatePresence>
              {liveAlerts.map((alert) => (
                <motion.div
                  key={alert.alertId}
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  className="p-4 border border-border-color hover:border-foreground/50 transition-colors"
                >
                  <div className="flex justify-between items-start mb-2">
                    <span className={`font-mono text-xs tracking-wider uppercase ${
                      alert.severity === "CRITICAL" ? "text-accent-red" : "text-warning"
                    }`}>
                      {alert.severity}
                    </span>
                    <span className="text-xs text-secondary-text font-mono">
                      {new Date(alert.triggeredAt).toLocaleString('en-US', {
                        month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
                      })}
                    </span>
                  </div>
                  <h3 className="font-bold mb-1">{alert.title}</h3>
                  <p className="text-sm text-secondary-text mb-4">{alert.description}</p>
                  <Link href={`/incidents/${alert.traceId}`}>
                    <Button variant="secondary" className="w-full text-xs py-2">View Incident</Button>
                  </Link>
                </motion.div>
              ))}
            </AnimatePresence>
            {liveAlerts.length === 0 && <p className="text-secondary-text font-mono text-sm">No active alerts.</p>}
          </div>
        </motion.section>

        <motion.section variants={containerVariants}>
          <h2 className="font-mono text-xl font-bold tracking-wider uppercase mb-6 border-b border-border-color pb-2">
            Recent Traces
          </h2>
          <div className="flex flex-col gap-4">
            <AnimatePresence>
              {liveTraces.map((trace) => (
                <motion.div
                  key={trace.traceId}
                  initial={{ opacity: 0, x: 20 }}
                  animate={{ opacity: 1, x: 0 }}
                  className="p-4 border-l-2 border-border-color hover:border-foreground/50 transition-colors"
                >
                  <div className="flex justify-between items-start mb-2">
                    <span className="font-mono text-xs tracking-wider uppercase text-secondary-text">
                      {trace.rootService} :: {trace.rootOperation}
                    </span>
                    <span className={`font-mono text-xs tracking-wider uppercase ${trace.status === "ERROR" ? "text-accent-red" : "text-secondary-color"}`}>
                      {trace.status}
                    </span>
                  </div>
                  <p className="text-sm font-mono mb-2 text-foreground/80 truncate">
                    {trace.status === "ERROR" ? `Failure Incident: ${trace.failureType.replace('_', ' ')}` : "Successful Execution"}
                  </p>
                  <div className="flex justify-between items-center mt-4">
                    <span className="text-xs text-secondary-text font-mono">
                      {new Date(trace.startedAt).toLocaleString('en-US', {
                        month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
                      })}
                    </span>
                    <span className="text-xs text-secondary-text font-mono">{trace.durationMs}ms</span>
                    <Link href={`/incidents/${trace.traceId}`}>
                      <span className="text-xs font-mono text-primary hover:text-primary-hover uppercase tracking-wider cursor-pointer">
                        Details →
                      </span>
                    </Link>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
            {liveTraces.length === 0 && <p className="text-secondary-text font-mono text-sm">No recent traces.</p>}
          </div>
        </motion.section>
      </div>
    </motion.div>
  );
}
