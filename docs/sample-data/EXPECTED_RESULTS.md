# Sample Data and Expected Results

These files are fictional and contain no pilot-company data.

## Required seed references

- Factory: `VLBG` - VLBG Demo Plant
- Line: `L-2` - Machining
- Machines: `M-04`, `M-07`
- Product: `P-03`
- Shifts: `MORNING`, `AFTERNOON`, `NIGHT`
- Downtime reasons: `TOOL_CHANGE`, `MATERIAL_WAIT`
- Scrap category: `DIMENSIONAL_DEFECT`

## Energy fixture

The first ten `M-04` morning records form a baseline. The 1 July value is
`68.800 kWh` while matching production remains approximately 100 units.

Expected after the abnormal row is imported:

- anomaly type `ENERGY_ABOVE_BASELINE`;
- observed value `68.800`;
- baseline calculated from the previous ten valid machine/shift periods;
- baseline quality is not `LOW`;
- severity is at least `HIGH`;
- recommendation uses the versioned template covering compressed air, motor
  temperature, idle running, and machine setup.

Whether this fixture reaches `CRITICAL` depends on the configured absolute
energy threshold. The contractual demo expectation remains `HIGH`; detector
tests set the threshold explicitly and do not rely on a hidden default.

## Scrap fixture

The night-shift `P-03` scrap count increases from 10 to 22 on `M-04`. The final
anomaly outcome depends on the configured scrap threshold and matching
production data. The detector test sets the threshold explicitly rather than
relying on a hidden default.

## Downtime fixture

The two rows prove import, reason resolution, filtering, and aggregation. They
are not sufficient by themselves to prove a recurring-downtime anomaly.
