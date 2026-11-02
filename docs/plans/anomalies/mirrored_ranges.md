# Anomaly: Mirrored Space

Something in the system bends weapon fire: every weapon's base range, on both sides, is flipped around 1000 su in every battle fought there. Short-ranged guns reach far and long-ranged guns become knife-fighters: point defence at 300 reaches 700, a 900 gun drops to 100, a 1200 gun reaches 800.

Status: proposed 2026-11-02 (community suggestion). Not built. See `overview.md` for the shared framework.

## How it plays

Both sides are mirrored, so it isn't a flat penalty; it rewrites which loadouts work. It still bites the player hardest, since player fleets are usually built around long range on purpose while chart fleets roll mixed loadouts. Seeing it on the atlas before entering gives the player the chance to refit around it.

The anomaly adds no fleets and no terrain.

## Effect

The new base range is `1000 - (base range mod 1000)`, which matches all three examples. Placeholder floor of 100 su, since a 950 gun would otherwise reach 50; exactly 1000 maps to 1000 (from `1000 - 0`).

It uses the spec's base range (`weapon.getSpec().getMaxRange()`) and changes only the base, through a `WeaponBaseRangeModifier` flat mod of `new base - old base`, as `MediumEnergySlotRangeListener` already does. Every percent and flat range bonus (Integrated Targeting Unit, skills, tree nodes, chart range lines) then applies to the mirrored base as normal, and range caps like Safety Overrides' still apply after. Missiles are left out: their reach comes from flight time, so a range change would only mislead the AI. Beams and fighter weapons are included, as are stations and modules.

## Where it lives

The anomaly class `MirroredSpace` in the `anomaly` root (single class) and its `chart_anomalies.csv` row. A battle plugin, `MirroredRangeBattlePlugin`, on in campaign battles while the player is in a mirrored system, adds `MirroredRangeListener` to every ship and fighter as it enters the battle (checked on a one-second timer over `engine.getShips()`, skipping ships that already have it). The formula is a static method for tests. The listener reads the spec range per call, so no per-frame work of our own.

## Rewards

The anomaly's `effects` column carries an existing loot line (placeholder: more weapons dropped, `CHART_WEAPON_PERCENT`), since the system pushes the player to refit.

## Open questions

1. Whether the refit screen should show mirrored ranges while in the system. Proposed no: the listener only exists in combat.
2. Whether to exclude Fear the Reaper (`fear_the_reaper.md`). Reapers are missiles, so they're unaffected; proposed no exclusion.

## Build order

1. [ ] `MirroredSpace`, its insertable `chart_anomalies.csv` row and atlas marker icon (placeholder).
2. [ ] `MirroredRangeBattlePlugin` and `MirroredRangeListener`, registered in `data/config/settings.json`.
3. [ ] Strings (en and zh_CN draft) following `docs/tooltip_wording.md`, for example "Weapon base ranges on both sides are mirrored around 1000: a 300 range weapon reaches 700, a 900 range weapon 100".
4. [ ] Tests for the formula and that missiles are skipped; the `overview.md` table row; the `docs/docs.md` section.
