import type { Metadata } from "next";
import { IBM_Plex_Mono } from "next/font/google";
import "./globals.css";
import AppContext from "@/context/AppContext";

const ibmPlexMono = IBM_Plex_Mono({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
  variable: "--font-ibm-plex-mono",
});

export const metadata: Metadata = {
  title: "Incident Management",
  description: "Real-time production incident response",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={ibmPlexMono.variable}>
      <body className={`bg-background text-foreground font-sans min-h-screen relative overflow-x-hidden antialiased`}>
        <AppContext>
          {/* Background Gradient */}
          <div className="fixed inset-0 bg-[radial-gradient(ellipse_at_center,_#1A1A1A_0%,_#0A0A0A_60%)] z-0 pointer-events-none"></div>
          
          {/* Main Content */}
          <main className="relative z-20 w-full min-h-screen flex flex-col">
            {children}
          </main>
        </AppContext>
      </body>
    </html>
  );
}
