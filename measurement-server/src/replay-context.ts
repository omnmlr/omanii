import { isDeepStrictEqual } from 'node:util';

// Task-local JSON mapping of approved Wave 1 context. Kotlin shared contracts remain the owner.
type Availability = 'AVAILABLE' | 'NOT_SUPPORTED' | 'PERMISSION_DENIED' | 'REDACTED' |
  'NOT_APPLICABLE' | 'TEMPORARILY_UNAVAILABLE' | 'UNKNOWN' | 'NOT_EVALUATED';
interface ValueState<T> { value: T | null; availability: Availability; reason: string | null; }
interface ScopedRef { session_id: string; }
export interface ReplayContextSnapshot {
  at_elapsed_realtime_ns: string; session_id: string;
  segment: ValueState<ScopedRef & { segment_id: string }>;
  network_epoch: ValueState<ScopedRef & { epoch_token: string }>;
  coordinate_frame: ValueState<ScopedRef & { frame_id: string }>;
  pose_observation_id: ValueState<string>;
}
export interface ReplayIntervalContext {
  start: ReplayContextSnapshot; end: ReplayContextSnapshot;
  boundaries: { at_elapsed_realtime_ns: string; reason: string;
    before: ReplayContextSnapshot; after: ReplayContextSnapshot }[];
  continuity: 'CONTINUOUS' | 'BROKEN' | 'UNKNOWN' | 'NOT_EVALUATED';
  movement_span_m: ValueState<number>;
}
const availability = ['AVAILABLE', 'NOT_SUPPORTED', 'PERMISSION_DENIED', 'REDACTED',
  'NOT_APPLICABLE', 'TEMPORARILY_UNAVAILABLE', 'UNKNOWN', 'NOT_EVALUATED'];
function object(value: unknown): Record<string, unknown> {
  if (typeof value !== 'object' || value === null || Array.isArray(value)) throw new Error('Invalid context object');
  return value as Record<string, unknown>;
}
function text(value: unknown): string {
  if (typeof value !== 'string' || !value.trim()) throw new Error('Invalid context identity/reason');
  return value;
}
function state(value: unknown): Record<string, unknown> {
  const result = object(value);
  if (!availability.includes(result['availability'] as string) || result['value'] === undefined ||
      (result['availability'] === 'AVAILABLE') !== (result['value'] !== null)) throw new Error('Invalid value state');
  if (result['reason'] !== null) text(result['reason']);
  return result;
}
function snapshot(value: unknown, nanos: (value: unknown) => bigint): ReplayContextSnapshot {
  const result = object(value); const session = text(result['session_id']); nanos(result['at_elapsed_realtime_ns']);
  for (const [key, token] of [['segment', 'segment_id'], ['network_epoch', 'epoch_token'], ['coordinate_frame', 'frame_id']] as const) {
    const field = state(result[key]);
    if (field['value'] !== null) {
      const ref = object(field['value']);
      if (text(ref['session_id']) !== session) throw new Error('Cross-session context reference');
      text(ref[token]);
    }
  }
  const pose = state(result['pose_observation_id']); if (pose['value'] !== null) text(pose['value']);
  return result as unknown as ReplayContextSnapshot;
}
export function contextIsComparable(
  raw: unknown, beginRaw: unknown, start: bigint, end: bigint, nanos: (value: unknown) => bigint,
): boolean {
  const context = object(raw); const first = snapshot(context['start'], nanos);
  const last = snapshot(context['end'], nanos); const begin = snapshot(beginRaw, nanos);
  const intervalStart = nanos(first.at_elapsed_realtime_ns); const intervalEnd = nanos(last.at_elapsed_realtime_ns);
  if (first.session_id !== last.session_id || intervalEnd < intervalStart) throw new Error('Invalid context interval');
  if (!Array.isArray(context['boundaries']) ||
      !['CONTINUOUS', 'BROKEN', 'UNKNOWN', 'NOT_EVALUATED'].includes(context['continuity'] as string)) throw new Error('Invalid context coverage');
  let previous = intervalStart;
  for (const value of context['boundaries']) {
    const boundary = object(value); const at = nanos(boundary['at_elapsed_realtime_ns']); text(boundary['reason']);
    const before = snapshot(boundary['before'], nanos); const after = snapshot(boundary['after'], nanos);
    if (before.session_id !== first.session_id || after.session_id !== first.session_id ||
        at < previous || at > intervalEnd || at < nanos(before.at_elapsed_realtime_ns) || at > nanos(after.at_elapsed_realtime_ns)) throw new Error('Invalid boundary evidence');
    previous = at;
  }
  if (context['boundaries'].length > 0 && context['continuity'] !== 'BROKEN') throw new Error('Hidden boundary');
  const movement = state(context['movement_span_m']);
  if (movement['value'] === null) text(movement['reason']);
  else if (typeof movement['value'] !== 'number' || !Number.isFinite(movement['value']) || movement['value'] < 0) throw new Error('Invalid movement');
  const stable = first.segment.availability === 'AVAILABLE' && first.network_epoch.availability === 'AVAILABLE' &&
    isDeepStrictEqual(first.segment.value, last.segment.value) && isDeepStrictEqual(first.network_epoch.value, last.network_epoch.value) &&
    isDeepStrictEqual(first.coordinate_frame, last.coordinate_frame) &&
    ['AVAILABLE', 'NOT_APPLICABLE'].includes(first.coordinate_frame.availability);
  if (context['continuity'] === 'CONTINUOUS' && !stable) throw new Error('Unproven continuous context');
  return stable && context['continuity'] === 'CONTINUOUS' && context['boundaries'].length === 0 &&
    intervalStart === start && intervalEnd === end && isDeepStrictEqual(first, begin);
}
