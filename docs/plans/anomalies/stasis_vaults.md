# Anomaly: Stasis Vaults

Domain stasis fields hold whole fleets frozen in place. You can leave a vault alone, or unfreeze it and fight the fleet inside for a Stasis Imprint, a new crafting currency that rerolls an item with one guaranteed modifier. Using a Corrupted Nanoswarm on the vault first wakes a stronger, corrupted fleet that drops a corrupted imprint. Modelled on Path of Exile's Essence league.

Status: planned, nothing built. See `overview.md` for the shared framework.

## How it plays

The chart places one to four vaults. Each holds a frozen fleet from the chart's faction pool, smaller than a normal chart fleet, and shows its strength and the imprint it holds before you touch it. Frozen fleets never move or join other battles, so each one is a fight you take only if you want it, at a time of your choosing.

At a vault you can:

- leave it;
- unfreeze it: the fleet wakes and attacks you at once, and winning drops its imprint;
- corrupt it with a Corrupted Nanoswarm before unfreezing: the fleet wakes stronger (fleet strength, officer level and quality up, placeholders), and drops a corrupted imprint. A corrupted vault can also wake as nothing, as a corruption outcome.

## Stasis Imprints

Imprints are crafting currency items. Each belongs to a modifier group (for example armour, flux, weapon range) and has a grade: Faint, Clear, Vivid and Perfect. Using one on a hull item or subroutine whose item type can roll that group rerolls the item as rare with one modifier from that group guaranteed at the grade's tier, and the rest rolled normally. Three imprints of the same group and grade combine into one of the next grade, except Perfect.

The chart's tier decides which grades its vaults hold. Corrupted imprints guarantee a modifier from a corruption-only list that can't roll any other way, and lock the item like any corrupted item.

Imprints are defined in `data/config/exiledSector/stasis_imprints.csv` (`id,name,group,grade,item_types,modifiers,min_tier,weight,note`), where `modifiers` lists `@id` entries from `item_modifiers.csv` per grade. Corrupted ones are rows with a `corrupted` grade. They're crafting currency like the ciphers: they live in `exiledsector-star-charts` and craft through core's `ItemCrafting`, so they need no hull upgrades code.

## Chart modifiers

- `CHART_STASIS_VAULT_CHANCE`: Area contains Stasis Vaults.
- `CHART_EXTRA_STASIS_VAULTS`: +N to stasis vaults.
- `CHART_IMPRINT_GRADE_CHANCE`: increased chance for vaults to hold a higher grade imprint.

## Build order

1. [ ] `stasis_imprints.csv`, the imprint currency, its use and combine rules, tests.
2. [ ] Vault entities with frozen fleets that never move, vault dialog.
3. [ ] Unfreeze fight and imprint drop.
4. [ ] Corruption on vaults and corrupted imprints.
5. [ ] Chart modifier lines, strings, icons.

## Open questions

- Which modifier groups get imprints at first, and whether subroutines take them at launch.
- Imprint names are working names; settle them alongside the nanoswarm and cipher theme.
