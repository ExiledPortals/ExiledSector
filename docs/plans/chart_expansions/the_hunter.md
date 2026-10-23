# Chart expansion: The Hunter

One named captain with a full skill tree built hard around a single theme stalks the system. Once it has sensor contact it hunts your fleet. You can evade it for the whole run with Starsector's sensor game, ambush it for an advantage, or face it for a strong drop. It's one fight at most, and you choose when.

Status: planned, nothing built. See `overview.md` for the shared framework.

## How it plays

The Hunter is a single fleet with a named commander and a title from its theme ("the Phase Stalker", "the Hive Mother"). Its ships get uncapped trees from `NpcSkillTreeBuilder` with the focus pushed far toward one theme tag group, so it feels unlike the chart's other fleets. Themes and their focus weights live in `data/config/exiledSector/chart_hunters.csv` (`id,name_key,theme_tags,focus_weight,ship_roles,min_tier,weight,note`).

It patrols until it gets sensor contact on you, then switches to a hunt: it pursues and uses Sustained Burn and Interdiction Pulse when it's in reach. Breaking contact sends it to your last known position, then it sweeps nearby, then it goes back to patrolling after a few days without contact. Going dark, nebulae, asteroid fields and staying out of its sensor range all work against it, as they do against any fleet. Intel shows it once it has spotted you, along with its theme, so you know what you're up against.

Catching it while it has no sensor contact on you is an ambush: in that battle, `ChartBattlePlugin` gives the Hunter's side reduced command points and increased deployment cost (placeholders).

## Rewards

The Hunter's flagship drops a Hunter's Modspec Cache: a Contraband Modspec Cache whose notables are rolled from that hunter's own allocated notables instead of the whole tree. It also drops extra loot budget scaled by tier.

## Chart modifiers

- `CHART_HUNTER_CHANCE`: Area contains a Hunter.
- `CHART_HUNTER_STRENGTH_PERCENT`: increased Hunter fleet strength.
- `CHART_HUNTER_REWARD_PERCENT`: increased Hunter rewards.
- The existing Hunted modifier (enemy sensor range) stacks with the Hunter.

## Build order

1. [ ] `chart_hunters.csv`, themed fleet and tree generation.
2. [ ] Hunt behaviour: patrol, pursue, search, give up.
3. [ ] Intel entry on contact.
4. [ ] Ambush detection and the battle hook.
5. [ ] Hunter's Modspec Cache drop, chart modifier lines, strings.

## Open questions

- The hunt AI must stay cheap: check contact on a timer or on vanilla sensor events, never per frame.
