"use client";

import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getIncidentDetail, acknowledgeAlert } from "@/lib/api-client";
import { motion } from "framer-motion";
import { containerVariants, itemVariants } from "@/lib/animations";
import { Button } from "@/components/Button";
import { Card } from "@/components/Card";
import Link from "next/link";
import { useEffect } from "react";
import { getStompClient } from "@/lib/ws";

export default function IncidentDetail({ params }: { params: { traceId: string } }) {
  const queryClient = useQueryClient();

  const { data, isLoading } = useQuery({
    queryKey: ["incident", params.traceId],
    queryFn: () => getIncidentDetail(params.traceId),
  });

  useEffect(() => {
    // Re-fetch incident when an analysis completes to show AI results automatically
    const client = getStompClient();
    const onConnect = () => {
      client.subscribe("/topic/analysis", (message) => {
        const payload = JSON.parse(message.body);
        if (payload.traceId === params.traceId) {
          queryClient.invalidateQueries({ queryKey: ["incident", params.traceId] });
        }
      });
    };
    if (client.connected) onConnect();
    else client.onConnect = onConnect;
  }, [params.traceId, queryClient]);

  const handleAcknowledge = async () => {
    if (data?.alert) {
      await acknowledgeAlert(data.alert.alertId);
      queryClient.invalidateQueries({ queryKey: ["incident", params.traceId] });
    }
  };

  if (isLoading) return <div className="min-h-screen flex items-center justify-center"><div className="w-8 h-8 border-t-2 border-foreground animate-spin rounded-full" /></div>;
  if (!data) return <div className="text-center mt-20 font-mono text-accent-red">Incident Not Found</div>;

  return (
    <motion.div initial="hidden" animate="visible" exit="exit" variants={containerVariants} className="max-w-6xl w-full mx-auto p-6 md:p-12">
      <motion.div variants={itemVariants} className="mb-8 flex justify-between items-end border-b border-border-color pb-6">
        <div>
          <p className="font-mono text-xs uppercase tracking-wider text-secondary-text mb-2">Trace Analysis</p>
          <h1 className="font-mono text-3xl font-bold tracking-wider uppercase text-foreground">{data.trace.status === "ERROR" ? `Failure Incident: ${data.trace.failureType.replace('_', ' ')}` : "Successful Execution"}</h1>
        </div>
        <div className="flex gap-4">
          <Link href="/incidents"><Button variant="secondary">History</Button></Link>
          <Link href="/"><Button variant="secondary">Dashboard</Button></Link>
        </div>
      </motion.div>

      <div className="grid grid-cols-1 lg:grid-cols-[1.2fr_0.8fr] gap-8">
        <motion.div variants={containerVariants} className="flex flex-col gap-6">
          <Card title="Trace Details">
            <div className="grid grid-cols-2 gap-4 font-mono text-sm">
              <div><span className="text-secondary-text block mb-1">Status</span><span className={data.trace.status === "ERROR" ? "text-accent-red" : "text-secondary-color"}>{data.trace.status}</span></div>
              <div><span className="text-secondary-text block mb-1">Duration</span><span>{data.trace.durationMs}ms</span></div>
              <div><span className="text-secondary-text block mb-1">Root Service</span><span>{data.trace.rootService}</span></div>
              <div><span className="text-secondary-text block mb-1">Failure Type</span><span>{data.trace.failureType}</span></div>
            </div>
          </Card>

          {data.trace.spans && (
            <Card title="Span Waterfall">
              <div className="flex flex-col gap-3">
                {data.trace.spans.map((span) => (
                  <div key={span.spanId} className="border border-border-color p-3 text-sm font-mono">
                    <div className="flex justify-between text-secondary-text mb-2">
                      <span>{span.serviceName} :: {span.operation}</span>
                      <span className={span.status === "ERROR" ? "text-accent-red" : ""}>{span.durationMs}ms</span>
                    </div>
                    {span.events.map(ev => (
                      <div key={ev.timeUnixNano} className="text-xs text-accent-red mt-1 ml-4 border-l border-accent-red/30 pl-2">
                        {ev.name}: {JSON.stringify(ev.attributes)}
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            </Card>
          )}
        </motion.div>

        <motion.div variants={containerVariants} className="flex flex-col gap-6">
          {data.alert ? (
            <Card title="Triggered Alert">
              <div className="flex justify-between items-start mb-4">
                <span className="font-mono text-sm tracking-wider uppercase text-warning">{data.alert.severity}</span>
                <span className="font-mono text-sm tracking-wider uppercase">{data.alert.status}</span>
              </div>
              <h3 className="font-bold mb-2">{data.alert.title}</h3>
              <p className="text-secondary-text text-sm mb-6">{data.alert.description}</p>
              {data.alert.status === "OPEN" && (
                <Button variant="primary" className="w-full" onClick={handleAcknowledge}>Acknowledge</Button>
              )}
            </Card>
          ) : (
            <Card title="Triggered Alert"><p className="text-secondary-text text-sm">No alert generated for this trace.</p></Card>
          )}

          <Card title="AI Analysis">
            {!data.analysis ? (
              <div className="flex flex-col gap-2 p-4 border border-border-color border-dashed bg-black/30">
                <div className="font-mono text-sm tracking-wider uppercase text-secondary-text">
                  AI Analysis Engine Offline
                </div>
                <p className="text-xs text-secondary-text/80">
                  The automated root cause analysis service is currently unavailable or disabled in this environment. Manual investigation is required for this trace.
                </p>
              </div>
            ) : data.analysis.status === "FAILED" ? (
              <p className="text-accent-red font-mono text-sm">Analysis failed to generate.</p>
            ) : (
              <div>
                <p className="text-foreground/90 text-sm leading-relaxed mb-4">{data.analysis.rootCause}</p>
                <h4 className="font-mono text-xs uppercase text-secondary-text mb-2 tracking-wider">Recommendations</h4>
                <ul className="list-disc pl-5 text-sm text-foreground/80 space-y-1 mb-4">
                  {data.analysis.recommendations.map((rec, i) => <li key={i}>{rec}</li>)}
                </ul>
                <div className="font-mono text-xs text-secondary-text">Confidence: {data.analysis.confidenceScore}</div>
              </div>
            )}
          </Card>
        </motion.div>
      </div>
    </motion.div>
  );
}
