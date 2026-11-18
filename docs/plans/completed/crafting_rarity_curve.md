# Crafting rarity curve

Replaces the material-tier bands of `rarity_drop_bands.md` (its rules 4 to 6) for crafting item drops. With about
twenty crafting items, each item's weight already says how rare it is, so rarity reshapes the weights directly
instead of multiplying everything above common by one shift.

The bands had Path of Exile 3.30's currency problem: past a point every band above common grew at the same rate, so
their shares froze. At tier 15 with 400% increased rarity the Replicator Nanoseed was at three quarters of its ceiling
and Corrupted Nanoswarm was 39% of all crafting drops, so extra rarity stopped improving drops.

## Rules

1. Each crafting item's pick weight is `table weight × (weight / table weight) ^ (1 / rarity ^ 0.4)`, where the table
   weight is the sum of every row in `crafting_drops.csv` and 0.4 is `CraftingDrops.RARITY_FLATTENING`. At 1x rarity
   the weights are as written.
2. Higher rarity raises every item's share of the table, and the rarer the item the more it gains, so the share comes
   from the most common items. The order never changes and there is no point where more rarity stops helping; the
   limit is every item equally likely.
3. Rarity is the chart tier's `crafting_rarity` (`star_chart_tiers.csv`, 1x outside charts) times the increased rarity
   of the item's crafting item subcategory, so +30% is as strong at every tier.
4. Each tier's `crafting_rarity` gives the tier's drops the same average rarity (the mean of log(table weight /
   weight)) as its old band shift: 1x at tier 1, 1.33x at tier 3, 1.47x at tier 5, 1.57x at tier 8, 1.61x at tier 10
   and 1.66x at tier 15.
5. `MaterialTier` still sets drop sounds, colours and announcements, but no longer changes drop chances.

## Model results

From `docs/analysis/chart_loot/crafting_rarity_model.js`, chart fleet pool at tier 15:

| Increased rarity | Augmentation | Corrupted | Immutable | Replicator |
|---|---|---|---|---|
| 0% (bands) | 17.1% | 18.2% | 1.46% | 0.35% |
| 0% | 18.7% | 10.7% | 1.36% | 0.42% |
| 400% (bands) | 7.3% | 38.6% | 3.10% | 0.74% |
| 400% | 12.3% | 9.1% | 3.10% | 1.68% |
| 800% (bands) | 4.6% | 44.1% | 3.54% | 0.85% |
| 800% | 10.6% | 8.4% | 3.56% | 2.19% |

Matching each tier's average rarity moves about 7% of tier 15's drops from Corrupted to the commons and the rarest
items. Above 0% the curve's average drop rarity is higher than the bands' and the gap widens with rarity.

## Open questions

1. Whether the modifier-count roll should stop saturating too. The curve doesn't fit it: modifier counts aren't
   ordered by their chances (two modifiers is rarer than three), so flattening their chances would make rare items
   less likely, not more.

## Steps

1. Done: `CraftingDrops.weightAtRarity` (`exiledsector-core`, `exiledsector.items.drop`) replaces `shiftedWeight`;
   `LootMultipliers.tierCraftingRarity` and `craftingRarity` replace the band shift. Tests in `CraftingDropsTest`.
2. Done: `crafting_rarity` replaces `band_shift` in `star_chart_tiers.csv`, read by `ChartTiers.craftingRarity` and
   `ChartLootContext.tierCraftingRarity`; officer rewards weight crafting items by `CraftingDrops.weightAtRarity`.
   Tests in `ChartTiersTest`, `ChartFleetLootTest`, `OfficerRewardPoolTest` and `SalvageSiteDropsTest`.
3. Done: `chart_loot_model.js` uses the curve; `crafting_rarity_model.js` sets the tier values; `docs/docs.md` Rarity
   section describes the curve.
