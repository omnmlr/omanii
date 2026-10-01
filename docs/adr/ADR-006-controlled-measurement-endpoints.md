# ADR-006 - Controlled measurement endpoints

**Status:** Accepted

## Context

Active network measurements depend on the tested path and server. Arbitrary third-party destinations have unknown capacity, caching, rate limits, consent models and availability. One overloaded test server can make thousands of healthy user connections look bad.

## Decision

Canonical omanii benchmark/probe profiles use controlled, versioned measurement endpoints with explicit health/context.

- The client records endpoint identity/region/protocol/version.
- Same-session comparisons use the same selected endpoint/path where possible.
- Client and server both enforce time/byte limits.
- Public-scale tests use scoped/signed bounded grants or equivalent abuse controls.
- The service must expose enough health/saturation evidence to invalidate tests when the endpoint is the bottleneck.
- The endpoint is not an arbitrary URL proxy.
- Packet loss, when offered, requires a protocol that can validly observe sequenced datagram loss; HTTP failure rate is not relabeled packet loss.
- A second independently placed endpoint may be used in experiments to detect endpoint-specific artifacts, but canonical score semantics remain explicit.

## Consequences

- More operational responsibility than using random public speed-test URLs.
- Measurement meaning is reproducible and testable.
- Endpoint geography/capacity must be part of score interpretation.
- Infrastructure cost/abuse controls become an engineering concern before broad distribution.

## Alternatives considered

### Use arbitrary popular websites

Rejected: no controlled semantics, consent, capacity or path stability.

### Depend entirely on a public measurement network

Useful as a reference in experiments but rejected as the canonical V1 dependency unless its consent/data/capacity model explicitly fits the product.

## Explicitly undecided

- cloud/provider and language for production service;
- number/regions of endpoints;
- exact grant mechanism;
- raw UDP deployment;
- adaptive endpoint selection strategy;
- production capacity targets.
