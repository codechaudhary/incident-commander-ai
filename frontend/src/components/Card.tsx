"use client";

import { motion } from "framer-motion";
import { itemVariants } from "@/lib/animations";
import { ReactNode } from "react";
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface CardProps {
  title?: string;
  children: ReactNode;
  className?: string;
}

export function Card({ title, children, className }: CardProps) {
  return (
    <motion.div
      variants={itemVariants}
      className={cn(
        "border-l-2 border-border-color pl-6 py-2 mb-8 transition-all duration-300",
        "hover:border-foreground/50 hover:shadow-[0_0_15px_rgba(234,234,234,0.05)]",
        className
      )}
    >
      {title && (
        <h2 className="font-mono text-xl text-foreground mb-4 uppercase tracking-wider">
          {title}
        </h2>
      )}
      <div className="font-sans text-foreground/80 text-lg">
        {children}
      </div>
    </motion.div>
  );
}
