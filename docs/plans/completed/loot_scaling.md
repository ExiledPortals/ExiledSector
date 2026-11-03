# Loot scaling

Every battle drop scales with the deployment points (DP) destroyed, through one table, then with difficulty and chart
modifiers. Today nanoswarms, chart ciphers, star charts and chart subroutines drop a flat amount per fleet, whatever its
size, while hull items scale with DP times enemy tree size times difficulty.

## Rules

1. Loot DP: the DP an enemy fleet lost in the battle. A chart fleet's loot is still claimed once through its ledger.
2. Curve: each drop has a rate per 100 loot DP and a curve. Linear drops scale 1:1. Diminishing drops run at full rate
   up to a knee, then grow with the square root of DP beyond it. The curve resets every battle.
3. Multipliers after the curve: battle difficulty (capped at 3x for table drops), chart modifiers (currency, item
   quantity and rarity, star chart, catalyst) and the boss's spoils multiplier.
4. Guarantees stay: boss unique, guaranteed tier-up chart, boss hull item. The boss's guaranteed crafting currency
   and two ciphers are gone.
5. A chart fleet's crafting materials come from the DP its ledger still owes, so rebuilt defenders can't be farmed.
   An anomaly's own fleet loot comes on top of the usual rewards. Endlessly launched station fleets drop only base
   feedstock. A won storm battle adds one hull item with a storm modifier.

Minor and Major nanoswarms are linear; Exotic and Mythic nanoswarms, inhibitors, hull items, subroutines and uniques
are diminishing. Each chart cipher drops at 1.5x the rate of a nanoswarm of its tier. Regular battles outside Star
Charts drop nanoswarms at half the chart rate and never drop ciphers.

## Star charts

Chart fleets drop about 1.5 star charts per whole chart at every tier: each fleet's share is its fleet points at spawn
over the fleet points its tier normally spawns (fleets, boss and the fleet limit's merging), so bigger fleets of the
same tier drop more. The run reward and system salvage stay on top. A chart drops within two tiers of the run: fleets
and salvage from two below to one above, the boss rarely two above.

## Steps

1. Done: core table `battle_loot.csv`, `LootCurve`, the difficulty cap (`BattleLootRates`) and the per-fleet loot
   rules a domain can set (`FleetLootRules`, `LootSource.fleetLootRules`).
2. Done: core rolls every crafting material per battle from the table, per enemy fleet, with that fleet's
   multipliers; feedstock also takes the fleet's feedstock multiplier (`BattleMaterials`, `ItemLootListener`).
3. Done: hull items read the table: enemy tree size raises the two-modifier share instead of the count; the knee curve
   and the difficulty cap apply; `battle_item_drops.csv` is gone (`BattleItemDrops`, `HullUpgradeLootSource`).
4. Done: chart fleets roll subroutines per DP, star charts and catalysts per expected fleet points, no materials of
   their own; the boss's chest keeps its items but loses its materials (`ChartFleetLoot`, `StarChartLootSource`).
5. Done: `chart_drops.csv` tier spread columns (two below to two above) replace `tier_up_chance` (`ChartDrops`).
6. Done: tests and `docs/docs.md` (Battle loot).
