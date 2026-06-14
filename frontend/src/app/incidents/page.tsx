"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getTraces, getAlerts } from "@/lib/api-client";
import { motion } from "framer-motion";
import { containerVariants, itemVariants } from "@/lib/animations";
import { Button } from "@/components/Button";
import Link from "next/link";
import { AlertDto } from "@/types";

export default function IncidentList() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);

  const { data: tracesData, isLoading: isLoadingTraces } = useQuery({
    queryKey: ["traces", page, size],
    queryFn: () => getTraces(page, size, "ERROR"),
    refetchInterval: 10000,
  });

  const { data: alertsData, isLoading: isLoadingAlerts } = useQuery({
    queryKey: ["alerts", "ALL"],
    queryFn: () => getAlerts(undefined, 100),
    refetchInterval: 10000,
  });

  const isLoading = isLoadingTraces || isLoadingAlerts;

  const getAlertBadge = (traceId: string) => {
    if (!alertsData) return null;
    const alert = alertsData.content.find(a => a.traceId === traceId);
    if (!alert) return null;

    return (
      <span className={`px-2 py-1 text-[10px] font-bold tracking-wider uppercase border ml-3 ${
        alert.severity === "CRITICAL" ? "border-accent-red text-accent-red" :
        alert.severity === "HIGH" ? "border-warning text-warning" :
        "border-primary text-primary"
      }`}>
        {alert.severity} ALERT
      </span>
    );
  };

  return (
    <motion.div
      initial="hidden"
      animate="visible"
      exit="exit"
      variants={containerVariants}
      className="max-w-6xl w-full mx-auto p-6 md:p-12"
    >
      <motion.div variants={itemVariants} className="mb-8 flex justify-between items-center border-b border-border-color pb-6">
        <div>
          <p className="font-mono text-xs uppercase tracking-wider text-secondary-text mb-2">Protocol Log</p>
          <h1 className="font-mono text-3xl font-bold tracking-wider uppercase">Incident History</h1>
        </div>
        <Link href="/">
          <Button variant="secondary">Dashboard</Button>
        </Link>
      </motion.div>

      {isLoading ? (
        <div className="w-8 h-8 rounded-full border-t-2 border-foreground animate-spin mx-auto mt-20" />
      ) : (
        <motion.div variants={containerVariants} className="flex flex-col gap-4">
          {tracesData?.content.length === 0 ? (
            <p className="text-secondary-text font-mono text-center mt-10">No error incidents found.</p>
          ) : (
            tracesData?.content.map((trace) => (
              <motion.div
                variants={itemVariants}
                key={trace.traceId}
                className="border border-border-color p-4 hover:border-foreground/50 transition-colors flex justify-between items-center group"
              >
                <div>
                  <p className="font-mono text-sm tracking-wider uppercase mb-1 flex items-center">
                    {trace.rootService} :: {trace.rootOperation}
                    {getAlertBadge(trace.traceId)}
                  </p>
                  <div className="flex gap-4 text-secondary-text font-mono text-xs mt-1">
                    <span>
                      {new Date(trace.startedAt).toLocaleString('en-US', {
                        month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false
                      })}
                    </span>
                    <span>|</span>
                    <span>
                      Failure Incident: {trace.failureType.replace('_', ' ')}
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-6">
                  <span className="font-mono text-xs tracking-wider uppercase text-accent-red">
                    {trace.status}
                  </span>
                  <span className="font-mono text-xs text-secondary-text">{trace.durationMs}ms</span>
                  <Link href={`/incidents/${trace.traceId}`}>
                    <Button variant="tertiary" className="!px-4 !py-2 text-xs">Inspect</Button>
                  </Link>
                </div>
              </motion.div>
            ))
          )}

          {/* Pagination Controls */}
          {tracesData && tracesData.totalPages > 0 && (
            <motion.div variants={itemVariants} className="mt-8 flex justify-between items-center border-t border-border-color pt-6">
              <div className="flex items-center gap-4">
                <span className="font-mono text-xs text-secondary-text">Show</span>
                <select 
                  className="bg-black border border-border-color text-foreground font-mono text-xs px-2 py-1 outline-none focus:border-primary"
                  value={size}
                  onChange={(e) => {
                    setSize(Number(e.target.value));
                    setPage(0);
                  }}
                >
                  <option value={10}>10</option>
                  <option value={20}>20</option>
                  <option value={50}>50</option>
                </select>
                <span className="font-mono text-xs text-secondary-text">per page</span>
              </div>
              
              <div className="flex items-center gap-6">
                <span className="font-mono text-xs text-secondary-text">
                  Page {tracesData.page + 1} of {tracesData.totalPages} (Total: {tracesData.totalElements})
                </span>
                <div className="flex gap-2">
                  <Button 
                    variant="secondary" 
                    className="!px-3 !py-1 text-xs"
                    onClick={() => setPage(p => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    Prev
                  </Button>
                  <Button 
                    variant="secondary" 
                    className="!px-3 !py-1 text-xs"
                    onClick={() => setPage(p => Math.min(tracesData.totalPages - 1, p + 1))}
                    disabled={page >= tracesData.totalPages - 1}
                  >
                    Next
                  </Button>
                </div>
              </div>
            </motion.div>
          )}
        </motion.div>
      )}
    </motion.div>
  );
}
