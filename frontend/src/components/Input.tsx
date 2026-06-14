"use client";

import { InputHTMLAttributes } from "react";
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
}

export function Input({ label, className, ...props }: InputProps) {
  return (
    <div className="flex flex-col gap-2 mb-8">
      <label className="font-mono text-secondary-text text-xs tracking-wider uppercase">
        {label}
      </label>
      <input
        {...props}
        className={cn(
          "bg-transparent border-0 border-b border-border-color p-2 pb-3 text-foreground",
          "focus:outline-none focus:border-foreground focus:shadow-[0_0_15px_rgba(234,234,234,0.1)]",
          "transition-all duration-300 font-sans placeholder-secondary-text/50",
          className
        )}
        style={{ caretColor: "var(--color-foreground)" }}
      />
    </div>
  );
}
