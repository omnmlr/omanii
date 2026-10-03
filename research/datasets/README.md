# Research datasets

**Current inventory: empty.** This scaffold creates no measurements, synthetic data or populated datasets. This directory catalogs provenance; it is not unrestricted raw-data storage.

## Preserve observations and provenance

- Raw measurements remain immutable within their authorized lifetime. Never silently fix observations; keep annotations and corrected/cleaned views separate.
- Derived/cleaned datasets identify input versions/checksums, transformation code/version, parameters, joins, exclusions and reasons. Manual annotations record author/date and source references.
- Preserve timestamps, units, clock domains and source-event meanings. Monotonic timing orders events within its valid domain; wall clock is display/history metadata, not clock alignment.
- Preserve scoped network/session/context identifiers, epochs, segments, coordinate frames and full probe boundary histories. Matching endpoints must not erase an A→B→A network-context transition; this differs from physical A→B→A visits.
- Preserve known measurement time separately from receipt, unavailable reasons, unknown age and independence. Never invent freshness or align ARCore frame time with elapsed realtime by assumption.
- Record device model/API, build/commit, capabilities/permissions, protocol/schema/profile/endpoint versions and coarse environment/procedure metadata. Identify manual notes and missing fields.
- Make dataset versions reproducible with manifests, checksums and exact transformations. Distinguish physical data, synthetic fixtures, pilot data and confirmatory evaluation.
- Retain failed, cancelled, partial and inconclusive runs and reasons; show attempted-run counts. Do not remove inconvenient outcomes to improve conclusions.

## Minimal catalog entry

Record dataset identifier/version, experiment and protocol revision, collection dates/scope, custodian, access-controlled storage reference, raw checksums, record-format/producer versions, device/build/environment context, limitations and related analyses. Include consent/export scope and approved retention/deletion arrangements. These are catalog details, not a new observation schema.

Large raw captures belong in bounded storage outside routine Git history. [Reusable sanitized replay/golden data](../../fixtures/README.md) belongs in fixtures through engineering ownership. [Engineering acceptance evidence](../../docs/evidence/README.md) remains with its task; reference rather than copy or rewrite it.

## Formats and privacy

[protocol/README.md](../../protocol/README.md) owns shared meaning. The [network](../../fixtures/network/README.md), [pose](../../fixtures/pose/README.md) and [probe](../../fixtures/probe/README.md) guides describe task-owned development formats, not a newly frozen unified research schema. Use matching parser/implementation versions. Preserve integer nanoseconds without precision loss in conversions.

Consolidated export, cross-stream association and additional metadata remain unresolved until the physically validated runtime is inspected. Unsupported fields stay unavailable with their analytical impact recorded. Do not invent replacement formats or extend shared contracts silently.

Follow [ADR-005](../../docs/adr/ADR-005-local-first-privacy.md): minimize sensitive information and prefer session-scoped tokens. Exclude raw wireless identities, exact locations/private interiors, credentials and unrelated usage from routine exports. Stable identifier hashes are not automatically anonymous. Preserve necessary scoped context without unnecessary identity.

Immutability is not indefinite retention. Approved deletion, participant withdrawal and access limits still apply. Where permitted, retain a nonsensitive deletion/provenance note so analyses disclose lost reproducibility. No new permissions, storage service, retention period or privacy default is established here.
