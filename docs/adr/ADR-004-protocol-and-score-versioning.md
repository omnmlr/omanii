# ADR-004 - Protocol and score versioning

**Status:** Accepted

## Context

omanii will change measurement procedures, record schemas and score calibration as evidence improves. Without explicit version axes, old results can become silently incomparable or be reinterpreted using formulas they were never measured to support.

## Decision

Persist explicit, separate version/context identifiers:

- `schema_version` for record shape/semantics;
- `protocol_version` for the collection/aggregation procedure;
- `test_profile` for the named measurement profile and claim budget;
- `score_version` for each score model;
- endpoint identity/region/protocol context for active measurements.

Omanii Score and OG Score are separate model families with separate versions.

A schema migration must not silently imply benchmark comparability. UI/history may compare only results whose model/profile/endpoint semantics are declared comparable, or must label the difference.

Preserve sufficient raw statistics where reasonable so later score versions can be recomputed without pretending the original protocol collected missing evidence.

Serialization format is not frozen by this ADR.

## Consequences

- Scientific changes can be made explicitly rather than mutating historical meaning.
- Fixtures can test old/new compatibility.
- More metadata accompanies every meaningful result.
- Product/history UI must handle non-comparable results gracefully.

## Alternatives considered

### One global app version

Rejected. App release version does not identify measurement or scoring semantics precisely enough.

### Always recalculate history with newest formula

Rejected unless the old session contains sufficient compatible statistics; otherwise it creates false comparability.

## Explicitly undecided

- serialization (JSON/Proto/etc.);
- semantic-versioning syntax;
- exact compatibility window;
- migration tooling;
- production score version numbers beyond the experimental bootstrap notation.
