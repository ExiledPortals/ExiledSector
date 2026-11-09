# Chart drawback: Recoiling (weapons shove their ships backwards)

Every shot from a player-side weapon kicks its ship backwards while fighting in the chart system, and the smaller the hull, the harder the kick. Frigates firing a big gun get thrown around; capitals only drift.

Status: proposed 2026-11-02 (community suggestion: "All weapons cause insane recoil", meaning a physical kick). Not built.

## Effect

One red-tier prefix line, `CHART_WEAPON_RECOIL_FLAT` (name Recoiling, placeholder `high=1`, a strength multiplier on the base kick). Each shot pushes the firing ship opposite to the weapon's facing, adding to `ship.getVelocity()`:

`kick = base kick x weapon size factor / hull size factor x line strength`

Placeholders: base kick 30 su/s, weapon size factor small 1 / medium 2 / large 4, hull size factor frigate 1 / destroyer 2 / cruiser 3 / capital 4. A large gun on a frigate kicks it 120 su/s per shot; the same gun on a capital, 30. Recoil alone never pushes a ship past its top speed (the kick is cut so the backwards component stays under it). Beams push continuously instead, at the same strength spread over each second of firing. Missiles don't kick. Fighters don't either, so a carrier's wings stay usable.

Turrets kick along wherever they point, so a broadside turns into sideways drift, and hardpoints always kick straight back, slowing a ship firing forward. Only the push along the weapon's line is applied, no spin.

## Where it lives

`ChartRecoilPlugin` in `charts.effect`, registered in `data/config/settings.json` beside `ChartMisfirePlugin`, on only in campaign battles while `ChartPlayerModifiers.weaponRecoil()` is above zero. The API has no per-shot event for every weapon, so it watches each armed weapon's cooldown on player-side ships: a jump from near zero to full means a shot (burst weapons count each burst as one shot, scaled by burst size). It reuses `ChartMisfirePlugin`'s once-a-second weapon list refresh, so per frame it reads one cooldown per weapon. The kick formula and shot detection are static helpers for tests.

## Open questions

1. Player side only, like the other red-tier lines. Proposed yes.
2. Whether ships that are phased still kick. Proposed no, they can't fire anyway.

## Build order

1. [ ] `CHART_WEAPON_RECOIL_FLAT` in `ChartModifier`, `star_chart_modifiers.csv` row, the strength through `ChartGenContext`, `ChartPlayerModifiers` (set, clear) and `ChartRunScript`.
2. [ ] `ChartRecoilPlugin` with shot detection and the kick formula.
3. [ ] Strings (en and zh_CN draft) following `docs/tooltip_wording.md`, for example "Weapons on your side <bad>shove their ship backwards</bad> when fired in the system, harder on smaller hulls".
4. [ ] Tests for the helpers; `docs/docs.md` "Chart system rules" sentence.
