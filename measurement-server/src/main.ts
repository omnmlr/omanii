import { measurementService, VERSION } from './server.ts';
const port = Number(process.env['PORT'] ?? '8787');
if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('Invalid PORT');
const { server } = measurementService();
server.listen(port, '127.0.0.1', () => {
  console.log(JSON.stringify({ ...VERSION, listen: `http://127.0.0.1:${port}`, exposure: 'loopback-only' }));
});
const stop = () => { server.close(); server.closeAllConnections(); };
process.once('SIGINT', stop);
process.once('SIGTERM', stop);
