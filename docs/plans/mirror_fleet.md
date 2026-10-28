# Endgame encounter: Mirror Fleet

A wormhole echo has copied your fleet: the same hulls and loadouts, the same skill trees, the same officers. You fight your own builds once. It's an endgame encounter, not an anomaly, and it's being explored, not settled.

Status: exploring, nothing built. Proposed 2026-10-28 as an anomaly, moved to an endgame encounter the same day.

## Lore

Deep in the Outer Reaches, a wormhole folds back on itself. Fleets that pass through it leave an echo behind, and the echo doesn't know it isn't the original.

## Where it appears

Not decided. The candidates:

1. Its own special site kind in `atlas_specials.csv` (`mirror`), far out (placeholder `min_steps` 20), needing a tier 15 chart, one attempt each. Repeatable across a save, since new ones keep appearing as the atlas grows.
2. The pinnacle encounter at the Gateway that the three Bastion key fragments open (`atlas.md`, Special sites), which still needs its own plan.
3. A unique system, one of the rule-bending systems in `atlas.md`.

Leaning: option 1, so the encounter tests whatever fleet you have now rather than once.

## The copy

The echo copies the ships you bring into the system, when the run opens. The more you bring, the stronger it is, so a lean fleet is a real choice; rewards scale with the copied fleet's points (floored, placeholder) so a lone frigate doesn't make it trivial.

- Hulls and variants: the same hulls, weapons, wings, hull mods and s-mods, cloned from each member's variant.
- Trees: each player ship's `ShipSkillData` encoded with `NpcTreeTag.encode` onto its copy's variant, so the copy's allocations, options and level resolve through `SkillDataResolver.npcRecord` like any NPC tree.
- Items: `NpcTreeTag` only carries NPC item ids (`NpcItems.isNpcId`), so the player's socketed hull items and subroutines don't come across today. See Open questions.
- Officers: copies with the same skills and level, and the commander gets the player's fleet-wide skills.
- CR: the copies start at full CR.

## The fight

One battle on a battle map of ours (placeholder: a mirrored starfield background). The echo never retreats. Copies get a visual tint so the two sides can be told apart at a glance.

## No recovery

No copy can ever be recovered, by normal or story-point recovery, so the encounter can't be used to clone your own ships. Every copy's variant gets vanilla's `Tags.VARIANT_UNBOARDABLE`, which `FleetEncounterContext.getRecoverableShips` checks first (through `Misc.isUnboardable`) and skips both kinds of recovery for. The echo fleet also gets `MemFlags.MEMORY_KEY_NO_SHIP_RECOVERY` as a second guard. A test checks that every copy carries the tag.

## Rewards

The flagship drops a Contraband Modspec Cache whose notables are rolled from your own fleet's allocated notables instead of the whole tree. Further drops are open.

## Open questions

- Where it appears (above).
- Copying player items: give the copy NPC-side copies of each socketed item with the same rolls (a new `NpcItems` kind that never drops), or leave items off and make the echo stronger some other way.
- Ships that can't copy cleanly: station modules, mod hulls whose scripts depend on campaign state, ships whose hull mods are granted by the player's own nodes.
- Whether copying what you bring invites cheese, and whether the echo should copy your whole fleet instead.
- Whether the copies' weapons and wings should drop as salvage, since that also copies your loadouts, or be held back like the hulls.
- Whether other mods' boarding or capture mechanics bypass vanilla's recovery check, and need their own guard.
- Rewards beyond the Modspec Cache. A Replicator Nanoseed already makes mirrored item copies, so a mirrored copy of your own item would repeat it.
- The name clashes with mirrored items (Replicator Nanoseed copies). "Echo Fleet" is an alternative.
