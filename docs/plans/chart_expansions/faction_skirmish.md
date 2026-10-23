# Chart expansion: Faction Skirmish

A weaker allied faction holds a foothold in the system and is losing a war against the chart's faction. You don't pick a side: the allies are on yours, and the boss leads the enemy. Their fleets soak up damage and CR that you would otherwise lose, so the system can hold more fights than a normal chart without wearing your fleet out.

Status: planned, nothing built. See `overview.md` for the shared framework.

## How it plays

The chart's faction stays the enemy. The allied faction is any faction hostile to it and not hostile to the player when the chart opens, picked by weight from `data/config/exiledSector/chart_skirmish_allies.csv` (`enemy_faction,ally_faction,weight,note`), falling back to independents. The allies get a foothold (a small station or outpost) and a few fleets, together around half the enemy's strength (placeholder).

Enemy fleets push on the foothold on their own: they attack allied fleets and the station, and those battles are fought between NPCs in the campaign layer. You can join any of them, and joining puts you on the allied side because of the faction relations. Fleets that come out of a fight keep their damage and lost CR, so a battle the allies have already been in is easier to finish.

The run tracks a front: how much of each side is left. As the enemy's other fleets fall, the boss commits and moves on the foothold. That battle has the allied fleets nearby joining on your side, so the boss fight is bigger but shared.

If the foothold falls, the allies are gone and the chart plays out as normal.

## Rewards

While the foothold stands you can dock there. It opens a temporary submarket whose stock is rolled from the chart's loot table and grows with how much of the allied side survives, plus a quartermaster reward when the boss falls, also scaled by allied survival. Helping also gives a small reputation gain with the allied faction in the real sector.

## Chart modifiers

- `CHART_SKIRMISH_CHANCE`: Area contains a Faction Skirmish.
- `CHART_ALLY_STRENGTH_PERCENT`: increased allied fleet strength.
- `CHART_ALLY_REWARD_PERCENT`: increased allied rewards.
- `CHART_ALLY_STOCK_PERCENT`: increased allied market stock.

## Build order

1. [ ] Ally picker, `chart_skirmish_allies.csv`, allied foothold and fleets.
2. [ ] Enemy assignments that attack the foothold and allied fleets; front tracking in the run.
3. [ ] Boss commitment once the front tips.
4. [ ] Foothold submarket and quartermaster reward.
5. [ ] Chart modifier lines, strings, intel showing the front.

## Open questions

- Confirm that NPC-vs-NPC campaign battles in a chart system resolve and leave damage and CR loss the same way they do in the sector, and that the player can join one in progress, before step 2.
- Nexerelin may change relations mid-run; the allies' relation should be fixed for the chart's run.
