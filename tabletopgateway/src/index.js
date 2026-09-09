import { createApp } from './app.js';
import { resolveListenConfig } from './listenConfig.js';

const { port, host } = resolveListenConfig();

const app = createApp();
app.listen(port, host, () => {
  console.log(`tabletopgateway listening on http://${host}:${port}`);
});