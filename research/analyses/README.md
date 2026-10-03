# Analyses

An analysis should let another contributor see what was tested, reproduce it where practical and understand its limits. No analyses or results exist in this scaffold.

For each analysis, identify:

- Question, experiment/protocol revision, exact dataset/version/checksums and physical/pilot/synthetic origin.
- **Exploratory or confirmatory** status and when choices were made relative to viewing outcomes. Freeze a confirmatory plan before evaluation; pilot data used for tuning is not independent confirmation.
- Analysis unit, dependence assumptions, estimand/metric and units, comparison groups, missingness/exclusions and context/clock-alignment assumptions.
- Scripts/notebooks where practical, software versions, parameters/seeds and a reproducible command. Record calculation steps and transformations for manual work.
- Effect estimates, uncertainty and diagnostics, or why uncertainty cannot be estimated reliably.
- All attempted runs, exclusions, failures, negative and inconclusive results, with sensitivity to plausible alternate choices.
- Limits on transfer to other devices, rooms, days, paths and profiles.

Do not cherry-pick spots, windows, metrics or successful runs. Keep chronological views available so trends and network boundaries remain visible. Account for selection and multiple comparisons. Packets, repeated polls and render frames are not independent replicates without justification.

Statistical significance and product usefulness answer different questions. Report differences in meaningful units, intended benefit and resource cost. A small uncertain difference, an invalid comparison and convincing evidence of no useful difference must stay distinguishable.

Use [data provenance](../datasets/README.md) and link resulting [findings](../findings/README.md). Distinguish measured, interpolated and estimated support. Synthetic replay checks software semantics, not physical-location claims.
