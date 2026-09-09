export function resolveListenConfig(env = {}) {
  const inContainer = env.PORT !== undefined;
  const port = Number(env.PORT ?? env.GATEWAY_PORT ?? 3001);
  const host = env.GATEWAY_HOST ?? (inContainer ? '0.0.0.0' : '127.0.0.1');
  return { port, host };
}