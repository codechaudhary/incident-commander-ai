import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "var(--color-background)",
        foreground: "var(--color-foreground)",
        "secondary-text": "var(--color-secondary-text)",
        "border-color": "var(--color-border-color)",
        "accent-red": "var(--color-accent-red)",
        primary: "var(--primary-color)",
        "primary-hover": "var(--primary-hover)",
        secondary: "var(--secondary-color)",
        warning: "var(--warning-color)",
      },
      fontFamily: {
        mono: ["var(--font-ibm-plex-mono)", "monospace"],
        sans: ["system-ui", "sans-serif"],
      },
    },
  },
  plugins: [],
};
export default config;
