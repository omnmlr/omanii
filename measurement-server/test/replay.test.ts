import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { parseReplay, replay } from '../src/replay.ts';
const text = readFileSync(new URL('../../fixtures/probe/observations.json', import.meta.url), 'utf8');
test('all eight required synthetic cases replay and round-trip without losing nanoseconds', () => {
  const records = parseReplay(text);
  assert.equal(records.length, 8);
  assert.deepEqual(parseReplay(JSON.stringify(records)), records);
  assert.deepEqual(records.map(r => r.outcome), ['SUCCESS', 'TIMEOUT', 'CANCELLED', 'PARTIAL', 'BUDGET_EXHAUSTED', 'SERVER_INVALID', 'SUCCESS', 'ERROR']);
  const seen: string[] = []; replay(text, r => seen.push(r.case)); assert.equal(seen.length, 8);
  assert.equal(records[6]!.comparison_safe, false);
  assert.equal(records[7]!.http_application_rtt_nanos, null);
  assert.equal(records[0]!.start_nanos, '9007199254740993');
});
test('replay rejects packet loss, unsafe context, bad timestamps/bytes/version and false success', () => {
  for (const mutate of [
    (r: Record<string, unknown>) => { r['packet_loss'] = 0.3; },
    (r: Record<string, unknown>) => { r['end_nanos'] = '1'; },
    (r: Record<string, unknown>) => { r['actual_bytes'] = 9999; },
    (r: Record<string, unknown>) => { r['schema_version'] = 'future'; },
    (r: Record<string, unknown>) => { r['endpoint_validated'] = false; },
    (r: Record<string, unknown>) => { (r['context'] as Record<string, unknown>)['boundary_nanos'] = [r['start_nanos']]; },
  ]) {
    const rows = JSON.parse(text) as Record<string, unknown>[]; mutate(rows[0]!);
    assert.throws(() => parseReplay(JSON.stringify(rows)));
  }
});
test('upload replay requires matching server confirmation and bounded requested/uncertain bytes', () => {
  const base = parseReplay(text)[0]!;
  const upload = { ...base, test_profile: 'verification_low_data', probe_type: 'UPLOAD',
    lower_bound: true, http_application_rtt_nanos: null, server_upload_bytes: 1 };
  assert.equal(parseReplay(JSON.stringify([upload]))[0]!.server_upload_bytes, 1);
  for (const changes of [{ server_upload_bytes: 0 }, { unconfirmed_upload_bytes: 1 },
    { requested_bytes: 0 }, { actual_bytes: 0 }]) {
    assert.throws(() => parseReplay(JSON.stringify([{ ...upload, ...changes }])));
  }
  assert.throws(() => parseReplay(JSON.stringify([{ ...base, server_upload_bytes: 1 }])));
  assert.throws(() => parseReplay(JSON.stringify([{ ...base, requested_bytes: 2 }])));
});
