# ADR-003 - Spatial coordinate and session model

**Status:** Accepted

## Context

The hero experience needs enough geometry to associate movement with network evidence, but reliable whole-room reconstruction and cross-day registration are not established product capabilities. ARCore world understanding can drift or update; blank walls, poor light and rapid movement can degrade tracking.

## Decision

Each scan uses a **session-local coordinate origin** and one or more segments.

- Pose observations retain session-local x/y/z in metres plus orientation, tracking state, coordinate-frame ID and monotonic time.
- Horizontal analysis projects the pose into a local floor plane; height remains available where useful.
- Tracking loss pauses sample placement.
- A discontinuity that cannot be reconciled starts a new segment.
- Relevant network/AP/band/transport transitions also start/split segments or invalidate cross-segment comparison.
- Route, measured support, interpolated support, unknown regions and candidate locations are stored as separate concepts.
- The first implementation does not require centimeter accuracy, architectural walls, whole-room reconstruction or cross-session geometric alignment.

Named spots/reference comparisons are preferred to pretending that a saved map automatically aligns on another day.

## Consequences

- The spatial scanner can be built around the actual user decision rather than mapping architecture.
- Rendering remains visually flexible.
- Replay can test pose/network association without needing a camera recording.
- Cross-day map features remain future R&D instead of hidden assumptions.

## Alternatives considered

### Full 3D room reconstruction first

Rejected as unnecessary for the core decision and likely to add fragility, compute/battery cost and false precision.

### GPS or compass as room coordinates

Rejected for indoor room-scale use.

### Pure accelerometer dead reckoning fallback

Rejected as an unvalidated universal replacement; drift/correction needs its own evidence.

## Explicitly undecided

- analysis cell size;
- interpolation/smoothing;
- anchor/keyframe strategy details;
- multi-floor behavior beyond segmentation;
- cross-session registration;
- wall/door modelling;
- alternative pose providers later.
