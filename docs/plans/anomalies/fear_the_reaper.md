# Anomaly: Fear the Reaper

Every hostile fleet in the system has refitted for one doctrine: their small and medium missile slots carry Reaper torpedoes, and these Reapers track their targets.

Status: proposed 2026-11-02 (community suggestion; working name kept). Not built. See `overview.md` for the shared framework.

## How it plays

Reapers are unguided strike torpedoes (`reaper`, small missile, `missileType: ROCKET`) that kill whatever they hit and miss everything that moves. Making them track turns every enemy missile slot into a capital-killer, so point defence, flares, phasing and shields decide the fight. Fleets heavy on PD shrug it off and fleets that tank with armour suffer, which gives the player a reason to refit before entering.

The anomaly adds no fleets and no terrain; it changes the fleets the chart already has, so it fits the "raise the stakes of a fight the chart already has" kind in `overview.md`.

## The refit

Each hostile chart fleet in the system, bosses and anomaly fleets included, is refitted when it is generated and again after each inflation, the same hook `ChartEnemyInflationListener` uses to re-add the chart's hidden hull mod. In each ship's variant, every small or medium pure missile slot, and every small or medium hybrid slot currently holding a missile weapon, is refitted, whatever the OP cost, since a chart fleet's refit only needs to work in combat. Small slots get the tracking Reaper (vanilla `reaper`, small) and medium slots the tracking Typhoon (vanilla `typhoon`, the medium Reaper launcher, same `reaper_torp` projectile). Large missile slots, built-in weapons and modules' built-ins stay as they are.

## The tracking Reapers

Custom weapons, never vanilla's `reaper` or `typhoon` (a file at a vanilla path would replace it game-wide): `exiledSector_chart_reaper` and `exiledSector_chart_typhoon`, in `data/weapons/` with their own `.wpn` files and one shared `.proj` (`exiledSector_chart_reaper_torp`), copying vanilla's sprites, sounds, ammo and damage, with `missileType: MISSILE`, a modest turn rate (placeholder, enough to follow a cruiser, not a frigate at full burn) and the same AI hints. Both are tagged so they never drop as loot, are never sold and never show in codex or autofit (`weapon_data.csv` tags such as `no_drop`, `restricted`, `no_sell`, to confirm against vanilla's hint and tag names).

## Rewards

The anomaly's `effects` column carries an existing loot line, more weapons dropped (`CHART_WEAPON_PERCENT`, placeholder), so a Reaper system drops more missile launchers. Optional: real Reapers and Reaper blueprints weighted up in that system's weapon loot.

## With other anomalies

Combines with everything. With Minelaying (`../chart_drawbacks/enemy_minelaying.md`) and Minefield it makes a missile-PD-heavy system; that's intended stacking, not an exclusion.

## Checks before building

1. Whether a refit after inflation survives later reinflation and campaign autofit (vanilla can re-autofit NPC variants; our listener already handles re-adding the hidden hull mod, so the same point should work).
2. Whether NPC AI fires a guided Reaper well, or needs different AI hints than the unguided original.
3. The tag names that keep the custom weapons out of loot, markets, codex and autofit.

## Build order

1. [ ] The anomaly class (`exiledsector.charts.anomaly`, single class, so in the root) and its insertable `chart_anomalies.csv` row with the loot line in `effects`; atlas marker icon (placeholder).
2. [ ] The two custom weapons with their `.wpn` files, the shared `.proj` and `weapon_data.csv` rows.
3. [ ] The refit in the chart fleet generation and inflation path, with slot picking in a static helper for tests.
4. [ ] Strings (en and zh_CN draft), the `docs/docs.md` section, tests; in-game pass on the AI check.

## Open questions

1. Whether ships without small or medium missile slots get anything. Proposed no.
