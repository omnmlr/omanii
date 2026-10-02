# TASK-002 synthetic replay data

Every row and exported record here is synthetic software test data. No phone,
radio, transport freshness or physical behavior was observed to produce it.
Canonical meaning remains in `protocol/README.md` and TASK-002.

`repair-cases.tsv` is input to the production logger through a fake source.
Columns: case, elapsed ns, transport, synthetic network key, AP key, frequency MHz,
subscription key, serving-cell key (`-` for none), source receipt ns, RSSI dBm,
VPN flag, validation flag, availability, expected epoch, required transition reason.
`repair-golden.jsonl` is its deterministic development export. All identifiers
use a fixed **synthetic-only** salt; production providers generate ephemeral salts.

The JUnit replay checks expected boundaries and exact serializer output. The
evidence validation script parses JSON and checks availability, timing, metadata,
session/epoch, attribution, independence and privacy semantics recursively.
