export default function AuthBackground({ children }) {
  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-ink-900 px-4">
      <img
        src="/dragon-bg.jpg"
        alt=""
        aria-hidden="true"
        className="absolute inset-0 h-full w-full object-cover"
      />
      <div aria-hidden="true" className="absolute inset-0 bg-ink-900/70" />
      <div className="relative z-10 w-full">{children}</div>
    </div>
  )
}