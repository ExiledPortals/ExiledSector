# Anomaly: Scrap Armada

Every hostile fleet in the system flies wrecks: each enemy ship carries 5 d-mods, and its crews know how to fight in junk. The d-mods make the ships cheap to field and hard to finish off, so there are more of them on the field and they soak far more hull damage than they look like they should.

Status: proposed 2026-11-02 (community suggestion). Not built. See `overview.md` for the shared framework.

## Effect

Every ship in a hostile chart fleet in the system, bosses and anomaly fleets included, gets d-mods added until it has 5. A ship that can't take that many (too few eligible d-mods) keeps what it can take. Stations and their modules are left alone.

Each enemy ship then gets two effects scaled by its d-mod count, capped at 5. Both are simulated by our own code: we don't grant vanilla's or SotF's skills, so nothing depends on how either mod tunes them.

- Derelict Operations: deployment points 6% lower per d-mod, compounding (`0.94^dmods`, about 27% lower at 5).
- Derelict Contingent:
  - Each hit that reaches hull has a 15% chance per d-mod (75% at 5) to have its hull damage cut to 10%.
  - Max CR 3% higher per d-mod (15% at 5).
  - A ship with no shield (none fitted, or a phase ship) also gets 5% more residual armour per d-mod: the minimum armour fraction damage is calculated against, 25 points higher at 5, on top of vanilla's base.

All numbers are placeholders.

## Where it lives

- The anomaly class `ScrapArmada` in the `anomaly` root (single class) and its insertable `chart_anomalies.csv` row.
- The d-mods are added in the chart fleet generation and inflation path, the same hook `ChartEnemyInflationListener` uses to re-add the chart's hidden hull mod, through `DModManager.addDMods` and `DModManager.setDHull` with the fleet's seed, so a reinflated fleet ends up with the same d-mods.
- The per-d-mod effects are enemy-side chart lines stored on the fleet and applied through the hidden hull mod (`ChartEnemyShipEffects`), carried by the anomaly's `effects` column:
  - The deployment point mult goes in before ship creation, so the battle's deployment sees it.
  - Max CR and residual armour are stat mods: `getMaxCombatReadiness`, and `getMinArmorFraction` for shieldless ships.
  - The hull damage roll is a damage taken listener added at ship creation. On a hit that will reach hull, it rolls once and, on a success, sets a hull damage taken mult of 0.1 for that hit only, clearing it on the next frame.

## Rewards

The anomaly's `effects` column also carries an existing loot line, more feedstock dropped (`CHART_FEEDSTOCK_PERCENT`, placeholder), since the fleets are made of scrap. Ships recovered there keep their d-mods.

## Checks before building

1. That the hull damage cut fires only on the hull part of a hit and never stacks with itself across several hits in one frame.
2. That NPC fleets deploy at their raised max CR, so the CR bonus matters at all.
3. How the deployment point cut and Besieged (`CHART_ENEMY_DEPLOYMENT_COST_PERCENT`) stack when both are on one chart. Proposed: they multiply.

## Build order

1. [ ] `ScrapArmada`, its insertable `chart_anomalies.csv` row and atlas marker icon (placeholder).
2. [ ] The d-mod top-up in the generation and inflation path, with the count rule in a static helper for tests.
3. [ ] The enemy-side lines (`CHART_ENEMY_DMOD_DEPLOYMENT_COST_PERCENT`, `CHART_ENEMY_DMOD_HULL_DAMAGE_CHANCE_PERCENT`, `CHART_ENEMY_DMOD_MAX_CR_PERCENT`, `CHART_ENEMY_DMOD_RESIDUAL_ARMOR_PERCENT`, each per d-mod) in `ChartEffect`, applied through `ChartEnemyShipEffects`, and the hull damage listener.
4. [ ] Strings (en and zh_CN draft) following `docs/tooltip_wording.md`, for example "Enemy ships have 5 d-mods" and "Enemy ships have a 15% chance per d-mod to take 90% less hull damage from a hit"; the `overview.md` table row; the `docs/docs.md` section; tests for the compounding DP, the shieldless check and the damage roll.
