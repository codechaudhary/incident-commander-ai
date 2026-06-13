type AlertItem = {
  id: string
  severity: 'CRITICAL' | 'HIGH' | 'MEDIUM'
  service: string
  message: string
  triggeredAt: string
}

type MetricCard = {
  label: string
  value: string
  trend: string
  tone: 'emerald' | 'rose' | 'amber' | 'blue'
}

const metrics: MetricCard[] = [
  { label: 'Live traces', value: '145', trend: '+12% vs last hour', tone: 'blue' },
  { label: 'Open alerts', value: '7', trend: '2 critical', tone: 'rose' },
  { label: 'Avg. MTTR', value: '18m', trend: 'down 4m', tone: 'emerald' },
  { label: 'AI confidence', value: '94%', trend: 'root-cause ready', tone: 'amber' },
]

const alerts: AlertItem[] = [
  { id: 'AL-1042', severity: 'CRITICAL', service: 'payment-service', message: 'Error rate spiked to 18.4% during checkout retry burst.', triggeredAt: '2 min ago' },
  { id: 'AL-1041', severity: 'HIGH', service: 'inventory-service', message: 'p99 latency breached 980ms on stock lookup requests.', triggeredAt: '6 min ago' },
  { id: 'AL-1039', severity: 'MEDIUM', service: 'gateway', message: '5-minute anomaly detected in upstream dependency fanout.', triggeredAt: '11 min ago' },
]

const services = [
  { name: 'payment-service', status: 'Degraded', health: '78%' },
  { name: 'inventory-service', status: 'Stable', health: '91%' },
  { name: 'gateway', status: 'Watch', health: '84%' },
]

const timeline = [
  'Trace ingested from OTel span tree and normalized.',
  'Anomaly detector flagged 3 sigma deviation on checkout path.',
  'AI narration engine identified DB timeout as probable root cause.',
  'Postmortem summary queued for review and alert distribution.',
]

function App() {
  return (
    <main className="min-h-screen bg-[radial-gradient(circle_at_top,_#111827_0%,_#030712_42%,_#020617_100%)] text-slate-100">
      <section className="mx-auto flex w-full max-w-7xl flex-col gap-8 px-4 py-8 sm:px-6 lg:px-8">
        <header className="rounded-3xl border border-white/10 bg-white/6 p-6 shadow-2xl shadow-black/30 backdrop-blur xl:p-8">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
            <div className="space-y-4">
              <p className="text-sm uppercase tracking-[0.35em] text-cyan-300">Incident Commander AI</p>
              <h1 className="max-w-3xl text-4xl font-semibold tracking-tight text-white md:text-5xl">
                Real-time production incident response, with AI-driven root-cause insight.
              </h1>
              <p className="max-w-2xl text-slate-300 md:text-lg">
                Monitor traces, surface anomalies, and turn noisy incidents into fast, shareable summaries for your on-call team.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <button className="rounded-2xl bg-cyan-400 px-4 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-300">Open live dashboard</button>
              <button className="rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-semibold text-slate-100 transition hover:bg-white/10">View incident feed</button>
            </div>
          </div>
        </header>

        <section className="grid gap-6 md:grid-cols-2 xl:grid-cols-4">
          {metrics.map((item) => (
            <article key={item.label} className="rounded-3xl border border-white/10 bg-white/6 p-5 shadow-xl shadow-black/25 backdrop-blur">
              <div className="mb-4 flex items-center justify-between">
                <p className="text-sm text-slate-300">{item.label}</p>
                <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${
                  item.tone === 'blue' ? 'bg-cyan-400/10 text-cyan-200' :
                  item.tone === 'rose' ? 'bg-rose-400/10 text-rose-200' :
                  item.tone === 'emerald' ? 'bg-emerald-400/10 text-emerald-200' : 'bg-amber-400/10 text-amber-200'
                }`}>{item.trend}</span>
              </div>
              <p className="text-4xl font-semibold text-white">{item.value}</p>
            </article>
          ))}
        </section>

        <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
          <article className="rounded-3xl border border-white/10 bg-white/6 p-6 shadow-2xl shadow-black/25 backdrop-blur">
            <div className="mb-5 flex items-center justify-between">
              <div>
                <p className="text-sm uppercase tracking-[0.3em] text-cyan-300">Latest incident</p>
                <h2 className="mt-1 text-xl font-semibold text-white">Checkout retry burst</h2>
              </div>
              <span className="rounded-full bg-rose-500/15 px-3 py-1 text-xs font-semibold uppercase tracking-[0.25em] text-rose-200">Critical</span>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Trace ID</p>
                <p className="mt-2 text-lg font-semibold text-white">4bf92f3577b34da6a3ce929d0e0e4736</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Duration</p>
                <p className="mt-2 text-lg font-semibold text-white">842 ms</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Impacted services</p>
                <p className="mt-2 text-lg font-semibold text-white">payment-service, inventory-service</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                <p className="text-xs uppercase tracking-[0.3em] text-slate-400">Status</p>
                <p className="mt-2 text-lg font-semibold text-white">ERROR · 3 spans failed</p>
              </div>
            </div>

            <div className="mt-5 rounded-2xl border border-emerald-400/20 bg-emerald-400/8 p-4 text-sm text-emerald-100">
              AI narrative: “The failure pattern points to a downstream DB timeout in the payment path, with retries amplifying the latency spike.”
            </div>
          </article>

          <article className="rounded-3xl border border-white/10 bg-white/6 p-6 shadow-2xl shadow-black/25 backdrop-blur">
            <p className="text-sm uppercase tracking-[0.3em] text-cyan-300">Active alerts</p>
            <ul className="mt-4 space-y-4">
              {alerts.map((alert) => (
                <li key={alert.id} className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-white">{alert.service}</p>
                      <p className="mt-1 text-sm text-slate-300">{alert.message}</p>
                    </div>
                    <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.2em] ${
                      alert.severity === 'CRITICAL' ? 'bg-rose-500/15 text-rose-200' :
                      alert.severity === 'HIGH' ? 'bg-amber-500/15 text-amber-200' : 'bg-cyan-400/10 text-cyan-200'
                    }`}>{alert.severity}</span>
                  </div>
                  <p className="mt-3 text-xs uppercase tracking-[0.25em] text-slate-400">{alert.id} · {alert.triggeredAt}</p>
                </li>
              ))}
            </ul>
          </article>
        </section>

        <section className="grid gap-6 xl:grid-cols-[0.95fr_1.05fr]">
          <article className="rounded-3xl border border-white/10 bg-white/6 p-6 shadow-2xl shadow-black/25 backdrop-blur">
            <p className="text-sm uppercase tracking-[0.3em] text-cyan-300">Service health</p>
            <div className="mt-4 space-y-4">
              {services.map((service) => (
                <div key={service.name} className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                  <div className="flex items-center justify-between gap-3">
                    <div>
                      <p className="text-sm font-semibold text-white">{service.name}</p>
                      <p className="text-xs uppercase tracking-[0.25em] text-slate-400">{service.status}</p>
                    </div>
                    <p className="text-sm font-semibold text-cyan-200">{service.health}</p>
                  </div>
                  <div className="mt-3 h-2 rounded-full bg-white/10">
                    <div className="h-2 rounded-full bg-gradient-to-r from-cyan-400 to-emerald-400" style={{ width: service.health }} />
                  </div>
                </div>
              ))}
            </div>
          </article>

          <article className="rounded-3xl border border-white/10 bg-white/6 p-6 shadow-2xl shadow-black/25 backdrop-blur">
            <p className="text-sm uppercase tracking-[0.3em] text-cyan-300">AI analysis flow</p>
            <ol className="mt-4 space-y-4">
              {timeline.map((item, index) => (
                <li key={item} className="flex gap-4 rounded-2xl border border-white/10 bg-slate-950/50 p-4">
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-cyan-400/10 text-sm font-semibold text-cyan-200">{index + 1}</span>
                  <p className="text-sm text-slate-200">{item}</p>
                </li>
              ))}
            </ol>
          </article>
        </section>
      </section>
    </main>
  )
}

export default App
