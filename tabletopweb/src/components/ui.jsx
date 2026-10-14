const TONES = {
  error: { role: 'alert', className: 'bg-red-50 text-red-700 border-red-200' },
  warning: { role: 'status', className: 'bg-amber-50 text-amber-800 border-amber-200' },
  status: { role: 'status', className: 'bg-zinc-100 text-zinc-700 border-zinc-200' },
  success: { role: 'status', className: 'bg-emerald-50 text-emerald-800 border-emerald-200' },
}

/** Small inline banner for errors, hints, status and success messages. */
export function Alert({ tone = 'error', children, className = '' }) {
  const { role, className: toneClass } = TONES[tone] ?? TONES.error
  return (
    <p role={role} className={`rounded-md border px-3 py-2 text-sm ${toneClass} ${className}`}>
      {children}
    </p>
  )
}

/** Page-level heading with the display serif and an optional one-line subtitle. */
export function PageHeading({ title, subtitle, children }) {
  return (
    <div>
      <h1 className="font-display text-2xl font-semibold tracking-tight text-zinc-900">{title}</h1>
      {subtitle && <p className="mt-1 text-sm text-zinc-600">{subtitle}</p>}
      {children}
    </div>
  )
}

/** A raised content panel, matching the white cards used across the app. */
export function Card({ className = '', children }) {
  return (
    <section className={`rounded-xl border border-zinc-200 bg-white p-6 shadow-sm ${className}`}>
      {children}
    </section>
  )
}

/** Shared loading placeholder so every page says the same thing the same way. */
export function LoadingState({ message = 'Loading…' }) {
  return (
    <p role="status" className="text-sm text-zinc-600">
      {message}
    </p>
  )
}

/** Shared empty-state block: a short title, optional body and optional action. */
export function EmptyState({ title, children, action }) {
  return (
    <div className="rounded-xl border border-dashed border-zinc-300 bg-white/60 p-6 text-center">
      <p className="font-medium text-zinc-700">{title}</p>
      {children && <p className="mt-1 text-sm text-zinc-500">{children}</p>}
      {action && <div className="mt-4 flex justify-center">{action}</div>}
    </div>
  )
}
