# Anomaly: Anomalous Debris

From a distance it looks like salvage: drifts of wreckage scattered around the system. Up close, each one hides a silent fleet crewed by nothing but AI cores. They never move and never fire first. You decide whether you can take one. Corrupt one before you wake it, and it fights harder and drops a currency that rolls a modifier no other way can.

Status: planned, nothing built. Redesigned 2026-10-27 from Stasis Vaults: AI-core fleets of random factions that sit non-hostile inside debris that hides them until you're close, the debris visual only, and the currency only from corrupted fleets, each currency guaranteeing its own modifier. See `overview.md` for the shared framework.

## How it plays

The chart places one to four debris fields, each hiding a dormant fleet smaller than a normal chart fleet. Every ship in one is drawn from a random faction's hulls, so a fleet mixes factions, and every ship is captained by an AI core. The field is visual only: no derelicts and nothing to salvage.

Intel and the map show only the anomalous debris, never the fleet. The fleet stays hidden until your fleet is close to the debris, then shows like any other fleet.

Dormant fleets are non-hostile to the player and never move, attack first or join other battles, using vanilla's dormant fleet setup (`RemnantSeededFleetManager.initRemnantFleetProperties` with `dormant` true, as LOST_SECTOR's dormant Enigma fleets do). Attacking one turns that fleet hostile for the rest of the run, with no reputation change, reusing the Supply Convoy's turn-hostile-on-attack code (`supply_convoy.md`, step 2). The others stay dormant.

Interacting with a dormant fleet opens vanilla's fleet encounter dialog with two options of ours in it:

- Engage: the fleet wakes and the battle starts as usual; winning drops normal chart loot.
- Corrupt and engage, with the cost in brackets ("(1 Corrupted Nanoswarm)", the corruption kernel's display name through i18n): the kernel is spent, the fleet wakes stronger (fleet strength, AI core level and quality up, placeholders) and the battle starts; winning drops one of the new currency items alongside its loot. The option is greyed out, with a tooltip saying why, when your cargo holds no Corrupted Nanoswarm.

Leaving works as in any fleet encounter. Corrupting is the only way to get the currency.

The dialog hooks in the same way the wormhole's does: `ChartCampaignPlugin.pickInteractionDialogPlugin` picks our plugin for a dormant fleet at `MOD_SPECIFIC` priority. That plugin builds on vanilla's `FleetInteractionDialogPluginImpl`, either as a subclass that adds the two options or as a short first screen that hands off to it once you engage, whichever the decompiled class allows without copying it. The battle, loot and post-battle screens stay vanilla, configured through `FIDConfig` like `ChartThreatFleets`' threat fleets.

## The currency

Each currency item is tied to one modifier of its own, stronger than ordinary ones and found only in a currency-only modifier pool that no other roll draws from. Using it on a hull item or subroutine whose item type can take that modifier rerolls the item as rare, with that modifier guaranteed and three random modifiers alongside it (a rare item's full four, `AffixLayout.MAX_EFFECTS`, at most two per side). There's a single grade: each currency always grants its modifier at its own fixed strength.

The currency items are rows in a new `data/config/exiledSector/anomalous_debris_currencies.csv` (`id,name,modifier,item_types,min_tier,weight,note`), each naming one `@id` from the currency-only pool in `item_modifiers.csv`. Like the ciphers (`StarChartCurrency`) they live in `exiledsector-star-charts` and craft through core's `ItemCrafting`. The framework comes first with one or two placeholder modifiers; the real modifiers are designed later.

## Chart modifiers

Tuning lines are chart effects carried by an atlas source, per `overview.md`:

- `CHART_EXTRA_ANOMALOUS_DEBRIS`: +N to anomalous debris.
- `CHART_ANOMALOUS_DEBRIS_CURRENCY_QUANTITY_PERCENT`: increased currency dropped by corrupted dormant fleets.

## Build order

1. [x] The currency-only modifier pool in `item_modifiers.csv`, kept out of every other roll; `anomalous_debris_currencies.csv`, the currency items and their reroll through `ItemCrafting`; one or two placeholder modifiers; tests. Built: the pool is every modifier a currency row names, held in core `ReservedModifiers` (an item keeps a reserved id on load, `Item.explicitModifier` finds it outside the pool, and `ItemDataIntegrity.checkReservedModifier` makes listing one in any pool or implicit an error); star-charts `AnomalousDebrisCurrency` and `AnomalousDebrisCurrencies` (loaded in `StarChartsDomain.loadItems`, checked through `StarChartDataRules`); placeholder engrams `exiledSector_revenant_engram` (`HULL_PERCENT_ANOMALOUS_DEBRIS`, 15% increased hull) and `exiledSector_vigil_engram` (`BEAM_WEAPON_RANGE_PERCENT_ANOMALOUS_DEBRIS`, 25% increased beam range) in `special_items.csv` with vanilla AI core icons as placeholders. The Hull Item bench shows an extra currency tile only while you own one (`SocketWorkbenchPanel.hullBenchCurrencies`). Tests: `AnomalousDebrisCurrencyTest`, `AnomalousDebrisCurrencyDataTest`, `HullBenchCurrenciesTest`.
2. [ ] The anomaly class and `chart_anomalies.csv` row; the visual-only debris fields; dormant fleets of random-faction hulls with AI core captains, non-hostile and still, hidden until you're close.
3. [ ] Turning a fleet hostile on attack (reusing the convoy's code) and its normal chart loot.
4. [ ] The encounter dialog: Engage and Corrupt and engage with its bracketed cost, greyed out without a kernel; the stronger fleet and the currency drop.
5. [ ] Chart modifier lines, strings, icons, `docs/docs.md` section and tests.

## Open questions

- Decompile what `initRemnantFleetProperties` with `dormant` true sets, whether a dormant fleet wakes on approach, and how that combines with the convoy's non-hostile handling.
- Decompile `FleetInteractionDialogPluginImpl` to choose between subclassing it and handing off to it, and how a non-hostile fleet's encounter offers an attack.
- How to keep a fleet off the map and sensors until the player is close, and the reveal range.
- The visual-only debris field: a custom `BaseTerrain` drawing vanilla's debris art (the Foiler rules in `../atlas.md`), since vanilla's debris field is salvageable.
- Which AI core levels captain the ships at each tier.
- Whether an item rerolled with the currency is locked like a corrupted item. Decided as a placeholder: no. It stays an ordinary rare item that other currencies can still change, so Substitution or Corruption can remove the granted modifier. The granted modifier has a fixed value, so Recalibration leaves it alone. Inhibitors block the engram like Metamorph (`AnomalousDebrisCurrency.canUse`).
- The currency names, alongside the nanoswarm and cipher theme. Placeholder: engrams, the memory traces of the dormant AI cores (Revenant Engram, Vigil Engram), named after the modifier each grants.
