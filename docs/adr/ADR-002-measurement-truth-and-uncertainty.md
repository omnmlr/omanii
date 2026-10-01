# ADR-002 - Measurement truth and uncertainty

**Status:** Accepted

## Context

omanii can produce visually persuasive but scientifically misleading results if it collapses measured, cached, interpolated, stale, missing and estimated values into one display number. Network conditions vary over time, platform observations may be stale or redacted, and selection of the apparent “best” noisy point biases the winner upward.

The product must remain useful to nontechnical users without hiding these limits.

## Decision

Uncertainty is represented in domain data, not inferred by UI.

At minimum distinguish:

- observed/measured values;
- interpolated values;
- estimated/derived values;
- missing/unavailable values and reason where known;
- stale or unknown-age values;
- invalidated values/windows;
- lower-bound/provisional results.

Every observation records receive time and, when available, actual measurement time/age. Repeated polling is not automatically independent measurement.

Spatial field regions preserve support type: `MEASURED`, `INTERPOLATED`, or `UNKNOWN`.

The final comparison can return `win`, `tie`, `inconclusive`, or `invalid`.

A “better spot” recommendation requires stationary verification independent from the samples used to shortlist the candidate. High-confidence wording is unavailable when support is insufficient.

## Consequences

- UI can truthfully simplify while still having the state needed to explain limits.
- Some sessions end without a winner; this is expected behavior.
- More domain states and fixtures are required than a simple numeric heatmap.
- Confidence work can evolve without changing the core principle.
- Consumer animations cannot upgrade evidence level.

## Alternatives considered

### Always show the best available point

Rejected because it turns noise into false confidence and makes wrong recommendations a product feature.

### Hide uncertainty in “advanced mode”

Rejected. Technical details may be hidden, but result validity must affect the default result.

### Treat interpolation as equivalent to measurement

Rejected. Visual smoothing cannot create observations.

## Explicitly undecided

- exact confidence equation/categories;
- exact staleness thresholds by observation source;
- interpolation radius and smoothing;
- exact minimum effect size for production wording;
- final consumer labels/colors for uncertainty.
