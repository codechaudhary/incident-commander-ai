"use client";

import { HTMLMotionProps, motion } from "framer-motion";
import { buttonHoverEffect, buttonTapEffect } from "@/lib/animations";
import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

interface ButtonProps extends HTMLMotionProps<"button"> {
  variant?: "primary" | "secondary" | "tertiary";
}

export function Button({ variant = "primary", className, onClick, ...props }: ButtonProps) {
  const handleClick = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (onClick) onClick(e);
  };

  const baseStyles = "font-mono tracking-wider uppercase transition-all duration-300 disabled:opacity-30 disabled:cursor-not-allowed";
  
  const variants = {
    primary: "bg-foreground text-background py-4 px-8 font-bold hover:bg-[#CCCCCC]",
    secondary: "bg-transparent border border-border-color text-secondary-text py-3 px-6 hover:text-foreground hover:border-foreground",
    tertiary: "bg-transparent text-secondary-text py-4 px-8 hover:text-accent-red"
  };

  return (
    <motion.button
      whileHover={props.disabled ? {} : buttonHoverEffect}
      whileTap={props.disabled ? {} : buttonTapEffect}
      className={cn(baseStyles, variants[variant], className)}
      onClick={handleClick}
      {...props}
    />
  );
}
