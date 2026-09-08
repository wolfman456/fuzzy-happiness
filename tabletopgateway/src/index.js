import { createApp } from './app.js';

const port = Number(process.env.GATEWAY_PORT ?? 3001);
const host = process.env.GATEWAY_HOST ?? '127.0.0.1';

const app = createApp();
app.listen(port, host, () => {
  console.log(`tabletopgateway listening on http://${host}:${port}`);
});