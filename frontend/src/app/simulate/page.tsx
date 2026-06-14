"use client";

import { useState } from "react";
import { motion } from "framer-motion";
import { containerVariants, itemVariants } from "@/lib/animations";
import { Button } from "@/components/Button";
import { Card } from "@/components/Card";
import { simulatorApi } from "@/lib/api-client";
import { FailureType } from "@/types";
import Link from "next/link";
import { useRouter } from "next/navigation";

export default function Simulate() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState<string | null>(null);

  const handleSimulate = async (failureType: FailureType) => {
    setLoading(true);
    setSuccess(null);
    try {
      await simulatorApi.post("/simulate", {
        userId: "user_" + Math.floor(Math.random() * 10000),
        amount: Math.floor(Math.random() * 500) + 50,
        currency: "USD",
        failureType: failureType,
        delayMs: failureType === "SLOW_RESPONSE" ? 5000 : 0,
      });
      setSuccess(`Triggered simulation with failure: ${failureType}`);
      setTimeout(() => router.push("/"), 1500);
    } catch (e) {
      console.error(e);
      setSuccess("Failed to trigger simulation");
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div
      initial="hidden"
      animate="visible"
      exit="exit"
      variants={containerVariants}
      className="max-w-4xl w-full mx-auto p-6 md:p-12"
    >
      <motion.div variants={itemVariants} className="mb-8 flex justify-between items-center border-b border-border-color pb-6">
        <div>
          <p className="font-mono text-xs uppercase tracking-wider text-secondary-text mb-2">Order Simulation</p>
          <h1 className="font-mono text-3xl font-bold tracking-wider uppercase">Trigger Outage</h1>
        </div>
        <Link href="/">
          <Button variant="secondary">Dashboard</Button>
        </Link>
      </motion.div>

      <motion.div variants={containerVariants} className="flex flex-col gap-6">
        <Card title="Normal Traffic">
          <p className="text-sm text-secondary-text mb-4">Simulates a successful order flow. No alerts will be generated.</p>
          <Button 
            variant="secondary" 
            onClick={() => handleSimulate("NONE")}
            disabled={loading}
            className="w-full"
          >
            Trigger Success Trace
          </Button>
        </Card>

        <Card title="Performance Degradation">
          <p className="text-sm text-secondary-text mb-4">Injects a 5-second sleep in the inventory-service. Generates a SLOW_RESPONSE warning.</p>
          <Button 
            variant="primary" 
            onClick={() => handleSimulate("SLOW_RESPONSE")}
            disabled={loading}
            className="w-full bg-warning text-black hover:bg-warning/80"
          >
            Trigger Slow Response
          </Button>
        </Card>

        <Card title="Database Timeout">
          <p className="text-sm text-secondary-text mb-4">Simulates a DB lock in payment-service. Generates a HIGH severity alert.</p>
          <Button 
            variant="primary" 
            onClick={() => handleSimulate("DB_TIMEOUT")}
            disabled={loading}
            className="w-full bg-accent-red text-white hover:bg-accent-red/80"
          >
            Trigger DB Timeout
          </Button>
        </Card>

        <Card title="Fatal Runtime Exception">
          <p className="text-sm text-secondary-text mb-4">Throws a NullPointerException in order-service. Generates a CRITICAL alert.</p>
          <Button 
            variant="primary" 
            onClick={() => handleSimulate("RUNTIME_EXCEPTION")}
            disabled={loading}
            className="w-full bg-accent-red text-white hover:bg-accent-red/80 border-2 border-transparent hover:border-white"
          >
            Trigger Critical Failure
          </Button>
        </Card>

        {success && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="mt-4 p-4 border border-secondary-color text-secondary-color font-mono text-center">
            {success}
          </motion.div>
        )}
      </motion.div>
    </motion.div>
  );
}
