# Reusable research protocols

A research protocol is a repeatable experimental procedure: controls, observations, failure handling and supported claims. It does not replace measurement/wire semantics owned by [protocol/README.md](../../protocol/README.md).

Keep a procedure inside its experiment while it serves only that study. Extract it here when multiple experiments need substantially the same steps or a procedure needs independent versioning. Experiments should pin the protocol revision and record deviations rather than copy evolving instructions.

A reusable protocol should cover purpose/scope, prerequisites, instrumentation/capability limits, controlled conditions, procedure, metadata, resource/stopping rules, validity/exclusions, data handling and limitations. The [canonical experiment contract](../../docs/research/EXPERIMENTS.md) remains the method and promotion owner.

Potential future protocols:

- Stationary network measurement.
- A→B→A spatial comparison.
- Repeated-location trials.
- Temporal control.
- Cross-device comparison.
- Orientation/device-position testing.
- Wi-Fi/cellular transition testing.

None is implemented or validated here. The [draft baseline experiment](../experiments/EXP-010-baseline-location-repeatability.md) contains only its candidate procedure. Numerical settings remain TBD until justified; a study draft does not authorize changing production profiles.
