// JVM loopback integration fixture, never used by the local service entry point.
import { measurementService } from '../src/server.ts';
const { server } = measurementService({ chunkDelayMs: 30 });
server.listen(0, '127.0.0.1', () => {
  const address = server.address();
  if (address && typeof address !== 'string') console.log(address.port);
});
