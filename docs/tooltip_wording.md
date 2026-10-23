# Tooltip wording rules

Every modifier, effect and stat line the player reads follows these rules: skill tree nodes, hull items, subroutines, Star Charts, nanoswarm notices and settings tooltips. They are modelled on Path of Exile's modifier wording, adapted to Starsector. Node and item flavour descriptions are covered by the separate flavour rules and don't carry numbers at all.

Before writing a line, find out how the value is combined in code. The wording is decided by what the value stacks with, not by how the effect is named or what the designer intended.

## Less is more

Write the shortest line that tells the player what happens. Don't over-explain anything, and when in doubt, cut it. This applies to every player-facing text, including flavour descriptions, settings tooltips and notices.

- Don't add a bracketed reminder of what a stat or category covers: "maneuverability", not "maneuverability (acceleration, deceleration and turn rate)".
- Don't name what is already on screen. A refusal says "Augmentation Nanoswarm can't modify a unique chart", not "...a unique chart like" followed by its name.
- State the rule, not the mechanism behind it: "can't be used while an inhibitor is on", not "reshapes every modifier, so it can't be used while an inhibitor is protecting one".
- Don't repeat what the UI already shows elsewhere, such as rarity, where an item is used, or the item's own name.
- Describe an outcome rather than listing every case when the cases don't help the player decide: "Corrupts a Star Chart, modifying it unpredictably."

## Percentages: increased/reduced vs more/less

Use "increased" and "reduced" when the value is summed with every other source of the same stat before it is applied. Several 10% increased sources make 20% increased, applied once to the base.

Use "more" and "less" when the value is applied as its own multiplier on top of the stat's other sources. Two sources of 10% more make 1.1 x 1.1, and each multiplies everything else.

When several of our own sources of one effect are summed with each other first and the total is then applied as a single multiplier on top of the stat's outside sources (vanilla hull mods, skills, other mods), the line is still "more" or "less". Path of Exile does the same for its "more per X" modifiers. This covers the skill tree's MULT effects (`desc.mult.*`) and Star Chart sensor range.

| Combined in code as | Positive | Negative |
|---|---|---|
| summed with the stat's other sources | 15% increased flux capacity | 15% reduced flux capacity |
| its own multiplier | 15% more flux capacity | 15% less flux capacity |

Never print a negative "increased" or "more". Flip the word instead: "10% reduced", not "-10% increased".

"Increased" and "reduced" describe the stat, not whether the change is good. A penalty to damage taken is "15% increased damage taken" in `<bad>` colour.

Don't use "higher", "lower", "larger", "shorter", "richer" or "bigger" for a percentage change. They hide whether the value is additive or multiplicative.

"N% faster" and "N% slower" are allowed only when the value divides a time, so the thing happens in 1/(1 + N%) of the time, as in Path of Exile's "Buffs expire 50% faster". A value that scales a rate directly uses the rules above: "15% more combat objective capture rate", "15% increased ship system charge regeneration".

A share of one quantity turned into another is "N% of X is Y" or "equal to N% of X": "25% of shield damage taken is converted to soft flux", "raid strength equal to 10% of this ship's maximum crew capacity".

Per-stack and per-unit bonuses put the scaling after the stat: "5% increased missile damage for each Heartless stack", "1% increased weapon damage for every su/s of current speed".

## Flat values

A flat amount added to a stat is "+N to stat": "+250 to hull points", "+50 to sensor range". A flat reduction is "-N to stat".

A flat amount of percentage points added to a stat that is already a percentage is "+N% to stat": "+10% to maximum combat readiness", "+10% to enemy fleet quality". Don't write it as "10% increased", which would mean 10% of the current value.

A count of extra things is "N additional things": "2 additional enemy fleets", "6 additional points of interest", "1 additional officer". Don't use "more" for counts, because "more" means multiplicative.

Don't use "Adds X to Y" for anything except a damage range added to each hit, where X is the low end and Y the high end. If we ever add flat damage, it reads "Adds 10 to 20 kinetic damage".

## Value ranges

The normal tooltip shows only the rolled value. The roll range, such as "(15-25)", appears only in the expanded F1 view.

## Who the line applies to

By default a line applies to the ship that has the skill tree or the installed item, and the line names no subject: "15% increased armor".

Every other subject comes first in the line, in Starsector's lowercase style: "Your fleet has 20% less sensor range", "Enemy fleets have 2 additional officers", "Fighters launched from this ship take 10% less damage", "Enemy ships within 1000 su have...". A fleet-wide effect from a single ship always says so: "Ships in your fleet have..." or "Your fleet has...".

A Star Chart line that only applies inside the chart system ends with "in the system" or "while in the system". Item quantity lines say "found in the system", which covers salvage sites, caches and defeated fleets alike.

## Categories

When a roll affects a whole category, name the category, not its members. "51% increased quantity of currency items found in the system", not "nanoswarms, Inhibitors and Ciphers". The category names are fixed, so the same group is always called the same thing everywhere:

| Category | Covers |
|---|---|
| items | everything the player can pick up |
| currency items | nanoswarms, Inhibitors and Ciphers |
| commodities | supplies, fuel, metals and every other vanilla commodity |
| Bridge items, Reactor items, ... | hull items for one section |
| colony items | vanilla colony items (nanoforges, synchrotrons, ...) |

Several separate categories in one roll are listed: "hull items, subroutines and Star Charts", "weapons and fighter wings", "blueprints and modspecs".

## Banned words

Never write "loot", "spoils" or "rewards" for things the player picks up. Use "items" with a category, "salvage" for vanilla's post-battle salvage screen, and "completion reward" only for the one-time Star Chart objective reward. Item quantity is always "quantity of items found in the system" or "quantity of items dropped by Star Chart Bosses".

## Chances

A flat chance is "N% chance to X" or "N% chance for X". A change to an existing chance follows the percentage rules above: "15% increased chance for the system to contain a colony item".

## Conditions and scaling

Conditions go after the value and stat: "while phased", "while in the system", "for each Heartless stack", "for every su/s of current speed", "up to a maximum of N".

## Capitalisation

Follow Starsector's own style. Stats and generic nouns are lowercase (flux capacity, sensor range, items, currency items, points of interest). Proper names keep their capitals: Star Charts, Star Chart Bosses, Nanite Feedstock, Bridge items, Domain, hull mod and faction names.

## Chinese

The same choice is made in `zh_CN.json`: 提高/降低 for increased/reduced, 提高/降低 ...（乘算） for more/less, 额外 for additional counts, and +N for flat values. See `docs/i18n/glossary-zh_CN.md`.
