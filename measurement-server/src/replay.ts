// Experimental fixture representation only; canonical owners: protocol/README.md, ADR-004.
import { contextIsComparable, type ReplayContextSnapshot, type ReplayIntervalContext } from './replay-context.ts';
export interface ReplayRecord {
  fixture: 'synthetic'; case: string; schema_version: 'probe-wave1-alpha-1';
  protocol_version: 'http-probe-alpha-1'; test_profile: string; profile_version: string;
  endpoint_context: { endpoint_id: string; service_version: string; region: string; protocol_type: 'HTTP/1.1' };
  probe_id: string; sequence: number; probe_type: 'RESPONSIVENESS' | 'DOWNLOAD' | 'UPLOAD';
  start_nanos: string; end_nanos: string; received_at_nanos: string; measurement_at_nanos: string | null;
  requested_bytes: number; actual_bytes: number; http_bytes_charged: number; unconfirmed_upload_bytes: number;
  outcome: 'SUCCESS' | 'TIMEOUT' | 'ERROR' | 'CANCELLED' | 'SERVER_INVALID' | 'PARTIAL' | 'BUDGET_EXHAUSTED';
  endpoint_health: string | null; endpoint_validated: boolean;
  begin_context: ReplayContextSnapshot; context: ReplayIntervalContext;
  comparison_safe: boolean; lower_bound: boolean; connection: 'COLD' | 'REUSED' | 'NOT_OPENED';
  concurrent_load: 'NOT_EVALUATED'; http_application_rtt_nanos: string | null;
  server_upload_bytes: number | null; reason: string | null;
}
const isObject = (value: unknown): value is Record<string, unknown> =>
  typeof value === 'object' && value !== null && !Array.isArray(value);
function nanos(value: unknown): bigint {
  if (typeof value !== 'string' || !/^\d+$/.test(value) || BigInt(value) > 9223372036854775807n) throw new Error('Invalid nanoseconds');
  return BigInt(value);
}
export function parseReplay(text: string): ReplayRecord[] {
  const data: unknown = JSON.parse(text);
  if (!Array.isArray(data) || data.length === 0 || data.length > 1000) throw new Error('Invalid replay');
  const ids = new Set<string>();
  for (const item of data) {
    if (!isObject(item) || item['fixture'] !== 'synthetic' || item['schema_version'] !== 'probe-wave1-alpha-1' ||
        item['protocol_version'] !== 'http-probe-alpha-1') throw new Error('Unsupported fixture version');
    if ('packet_loss' in item || 'score' in item) throw new Error('HTTP replay cannot supply packet loss or score');
    for (const key of ['case', 'test_profile', 'profile_version', 'probe_id']) {
      if (typeof item[key] !== 'string' || item[key].length === 0) throw new Error('Missing identity');
    }
    if (ids.has(item['probe_id'] as string)) throw new Error('Duplicate probe');
    ids.add(item['probe_id'] as string);
    if (!Number.isSafeInteger(item['sequence']) || (item['sequence'] as number) < 0) throw new Error('Invalid sequence');
    for (const key of ['requested_bytes', 'actual_bytes', 'http_bytes_charged', 'unconfirmed_upload_bytes']) {
      if (!Number.isSafeInteger(item[key]) || (item[key] as number) < 0) throw new Error('Invalid bytes');
    }
    if ((item['requested_bytes'] as number) < 1 ||
        (item['actual_bytes'] as number) + (item['unconfirmed_upload_bytes'] as number) > (item['requested_bytes'] as number) ||
        (item['http_bytes_charged'] as number) < (item['actual_bytes'] as number) + (item['unconfirmed_upload_bytes'] as number)) throw new Error('Invalid accounting');
    const start = nanos(item['start_nanos']); const end = nanos(item['end_nanos']);
    if (start > end || nanos(item['received_at_nanos']) < end) throw new Error('Invalid interval');
    if (item['measurement_at_nanos'] !== null && nanos(item['measurement_at_nanos']) !== end) throw new Error('Invalid source time');
    if (!['RESPONSIVENESS', 'DOWNLOAD', 'UPLOAD'].includes(item['probe_type'] as string) ||
        !['SUCCESS', 'TIMEOUT', 'ERROR', 'CANCELLED', 'SERVER_INVALID', 'PARTIAL', 'BUDGET_EXHAUSTED'].includes(item['outcome'] as string)) throw new Error('Invalid outcome/type');
    const endpoint = item['endpoint_context'];
    if (!isObject(endpoint) || endpoint['protocol_type'] !== 'HTTP/1.1' ||
        ['endpoint_id', 'service_version', 'region'].some(key => typeof endpoint[key] !== 'string' || endpoint[key] === '')) throw new Error('Invalid endpoint');
    const contextSafe = contextIsComparable(item['context'], item['begin_context'], start, end, nanos);
    const safe = item['outcome'] === 'SUCCESS' && item['endpoint_validated'] === true &&
      contextSafe;
    if (item['comparison_safe'] !== safe) throw new Error('Unsafe comparison');
    if (typeof item['endpoint_validated'] !== 'boolean' || typeof item['lower_bound'] !== 'boolean' ||
        !['COLD', 'REUSED', 'NOT_OPENED'].includes(item['connection'] as string) || item['concurrent_load'] !== 'NOT_EVALUATED') throw new Error('Invalid evidence');
    if (item['outcome'] === 'SUCCESS' && (item['actual_bytes'] !== item['requested_bytes'] ||
        item['endpoint_validated'] !== true || item['endpoint_health'] !== 'healthy')) throw new Error('Invalid success');
    if (item['outcome'] !== 'SUCCESS' && item['http_application_rtt_nanos'] !== null) throw new Error('Failure has no RTT');
    if (item['http_application_rtt_nanos'] !== null && (item['probe_type'] !== 'RESPONSIVENESS' || nanos(item['http_application_rtt_nanos']) !== end - start)) throw new Error('Invalid RTT');
    if (item['probe_type'] !== 'RESPONSIVENESS' && item['lower_bound'] !== true) throw new Error('Transfer claim exceeds methodology');
    if (item['server_upload_bytes'] !== null && (!Number.isSafeInteger(item['server_upload_bytes']) || (item['server_upload_bytes'] as number) < 0)) throw new Error('Invalid server bytes');
    if (item['probe_type'] !== 'UPLOAD' && (item['server_upload_bytes'] !== null || item['unconfirmed_upload_bytes'] !== 0)) throw new Error('Upload metadata on non-upload');
    if (item['probe_type'] === 'UPLOAD' && item['outcome'] === 'SUCCESS' &&
        (item['server_upload_bytes'] !== item['actual_bytes'] || item['unconfirmed_upload_bytes'] !== 0)) throw new Error('Unconfirmed successful upload');
    if (item['probe_type'] === 'RESPONSIVENESS' && item['requested_bytes'] !== 1) throw new Error('Invalid echo size');
    if (item['test_profile'] === 'low_data_responsiveness' && item['probe_type'] !== 'RESPONSIVENESS') throw new Error('Invalid profile class');
    for (const key of ['reason', 'endpoint_health']) if (item[key] !== null && typeof item[key] !== 'string') throw new Error('Invalid reason/health');
  }
  return data as ReplayRecord[];
}
export function replay(text: string, consume: (record: Readonly<ReplayRecord>) => void): void {
  for (const record of parseReplay(text)) consume(record);
}
