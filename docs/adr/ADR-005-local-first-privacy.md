# ADR-005 - Local-first privacy architecture

**Status:** Accepted

## Context

Spatial scans can reveal private interiors and network identities. BSSID/cell information can be location-sensitive. Usage history can reveal behavior. None of this data is required to operate a basic local decision tool, and centralizing it creates privacy, security and trust costs before a demonstrated business need.

## Decision

Default sensitive product state remains on-device.

- Camera frames/depth are processed for tracking and are not retained/uploaded by omanii by default.
- Room coordinates, labels and AP/cell identities remain local by default and are excluded from analytics/logs.
- Development/research exports prefer session-scoped tokens over stable wireless identifiers.
- Usage/per-app history, when later implemented, is local by default and isolated from advertising SDKs.
- Share outputs are sanitized/redacted by default.
- Product telemetry uses a small event taxonomy without raw map, SSID/BSSID/cell or app-inventory payloads.
- Android automatic cloud backup of sensitive map/database content remains disabled until an approved backup design exists.
- Any research capture of private interiors or richer identifiers requires separate explicit consent, bounded retention and deletion.

A hash of a stable BSSID is not treated as anonymous merely because it is hashed.

## Consequences

- V1 needs little cloud database infrastructure.
- Offline/basic value improves.
- Some future aggregate-intelligence ideas become opt-in research projects rather than default collection.
- Cross-device restore is deferred unless a secure user-controlled design is approved.

## Alternatives considered

### Cloud-first session storage

Rejected for V1: it creates risk/cost without being needed for the core user outcome.

### Hash identifiers and collect everything

Rejected: hashing does not remove linkability or location sensitivity.

## Explicitly undecided

- optional encrypted backup design;
- future account identity model;
- exact retention periods for production telemetry after legal/operational review;
- aggregate research/business models;
- cross-device sync.
