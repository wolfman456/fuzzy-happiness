export default function ThemedBackdrop({ children }) {
  return (
    <div className="relative flex min-h-screen flex-col overflow-x-clip">
      <img
        src="/dragon-bg.jpg"
        alt=""
        aria-hidden="true"
        className="fixed inset-0 h-full w-full object-cover"
      />
      <div aria-hidden="true" className="fixed inset-0 bg-zinc-950/60" />
      <div className="relative z-10 flex min-h-screen flex-col">{children}</div>
    </div>
  )
}