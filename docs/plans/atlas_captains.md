# Atlas captains

The Outer Reaches are frontier space the factions are fighting over. Each faction holds small holdings that start at 4 to 10 systems, each guarded by a named captain who persists across the save. Every time the player kills a chart boss, the atlas takes a turn: captains push into unclaimed space and into each other's holdings, challenge neighbouring captains, and rise or fall in rank. Beating a captain lets the player pick one of three items that captain will carry the next time they are beaten.

Status: designing, nothing built. Started 2026-11-12. Inspired by the Nemesis system of Middle-earth: Shadow of Mordor and Shadow of War (named, persistent enemy officers who remember the player, rise in rank and fight each other); never use that name. Builds on `atlas.md`, `completed/anomalies/faction_skirmish.md` and `anomalies/overview.md`.

## Holdings

A holding is a connected group of systems owned by one faction, with one seat system held by its captain. Holdings replace the large faction territories (`AtlasTerritories`, sites 16 cells apart, a couple of hundred systems each). Most of the atlas is unclaimed space between holdings.

Seats are placed when a chunk is first revealed. A seat's position is a pure function of the seed and the chunk (a hash per chunk, placeholder chance of a seat per chunk and a minimum seat spacing), but its faction depends on what the player can see, so it is decided at reveal and saved, like the Threat infestation decision for a chunk. The holding's starting claim is the seat plus the nearest systems by lanes up to a size rolled from 4 to 10, skipping completed systems, the Gateway, special sites, infested systems and other holdings. Claims are saved explicitly, since turns change them.

Unclaimed systems belong to independents and mercenaries: each one picks `independent` or `mercenary` on its cell hash (placeholder weights, today's `chart_factions.csv` 5 to 3). They have no captain and no border. Biomes whose faction pool holds only factions that never seat a holding (the Threat Expanse, Remnant Expanse, Abyssal Rift) keep their own factions in unclaimed systems. Elsewhere a region's faction pool no longer sets unclaimed systems' faction; it weights which factions can seat holdings there, so the Pirate Badlands fill with pirate holdings rather than pirate-owned empty space.

The 4 to 10 is only the starting size. A holding has no size limit: it grows by capturing systems on its turns and shrinks when it loses them. As a soft brake, a holding's chance to expand falls as it grows (placeholder curve). A holding that loses its seat breaks up (see Captains losing).

One reader returns a system's owner: infestation, then holding claim, then the unclaimed faction. `AtlasOpening.ownerOf` and `territoryOwnerOf`, the node tooltips, `AtlasTerritoryBorders`, `AtlasFleetTraffic` and Faction Skirmish all go through it. The Faction Territories toggle draws holding borders and each seat's captain.

## Factions coming and going

No faction is ever wiped out for good. The atlas is endless, and new holdings keep appearing in newly revealed space. A new seat's faction is weighted against what the visible atlas already holds:

- a faction with a fled captain waiting for a new holding (see Captains losing) gets the highest weight, so a captain the player just beat tends to turn up again further out and can be chased;
- a faction with no holding on the visible atlas gets a large boost;
- a faction already on the visible atlas is weighted down by how many holdings it has there.

All placeholders. The seat's region must still allow the faction (its faction pool). Remnant, derelict, Threat and Abyssal factions never hold holdings (reuse `chart_skirmish_factions.csv`); they stay infestations and biome landmarks. Independents and mercenaries never hold holdings either, since they are the unclaimed space.

Safeguards:

- a cap on holdings per faction on the visible atlas, so one faction can't paint the frontier;
- a minimum spacing between seats;
- a cap on live captains in the save;
- only holdings with a system in revealed space take turns, so unexplored space never simulates.

## Captains

Each holding has one captain, the boss of its seat system. A large or long-held holding may get a second-in-command who is the boss of another of its systems (placeholder, see Open questions). A captain is a named officer with a persistent identity: name, portrait, faction, rank, a personality trait, scars, the item they carry and a short history of their last few events. The fleet itself is rebuilt from the record every run, so chart systems keep the zero-trace rule.

A captain never moves on a turn. Expanding, pushing and challenging change claims and ranks, never seats, and a challenge winner keeps their own seat. A captain changes system only when they lose their seat (see Captains losing), so a player heading for a captain always closes in.

A captain record is one encoded string in `AtlasState` (as inserted anomaly records and capture records are), holding only ids and numbers: record id, faction id, name and portrait seeds, rank, seat cell, trait id, scar counts per theme tag, carried item id, pending choice, defeat count, status (active, fled, dead) and the last three history entries.

The comms greeting and the system tooltip refer to that history ("You ran me out of Kesteven's Reach"), templated through i18n with the atlas system names.

## Ranks

Ranks measure a captain's influence across the whole frontier, not a faction's chain of command. Five military ranks, lowest first: Lieutenant, Commander, Captain, Commodore, Admiral. Each rank row in a new `atlas_captain_ranks.csv` sets boss size, Fleet Strength (tree size), how often the captain acts each turn, how strongly they roll in a challenge, and a cap on how many live captains can hold it at once (placeholder: 2 Admirals, 5 Commodores, 10 Captains, the lower two uncapped). A higher rank pushes harder and wins more. A new holding's captain starts at one of the lower two ranks (placeholder weights).

A captain ranks up by beating another captain in a challenge, into the next rank only if it has a free place. A captain who beats someone of a higher rank swaps ranks with them, so the only way into a full rank is to beat a captain already in it. They rank down by losing a challenge or by being beaten by the player (see Captains losing). A captain at the lowest rank who loses a challenge dies for good, and a new captain of the same faction takes the holding at the lowest rank, with no scars or carried items.

## Turns

A turn happens each time a chart boss is defeated, in any atlas system: the moment the boss objective completes (`ChartObjectives.completed`, which calls `AtlasProgress.completed`), not when the run closes. A run where the player retreats, loses or leaves without killing the boss gives no turn.

The run is still open when its turn resolves, so the turn never touches the open run's system (`AtlasAnomalies.openRunCell`) or the captain fighting in it.

Captains declare their next action one turn ahead, so the board can show what each one intends. At the end of every turn, each active captain rolls its intent, weighted by rank and trait:

- Expand: claim an unclaimed system on the holding's border, less likely the larger the holding.
- Push: attack a border system of a neighbouring holding of another faction.
- Challenge: attack the neighbouring captain directly.
- Defend: a captain who is the target of a push or challenge may defend instead, adding to their roll against it.
- Hold: nothing.

A turn first applies the player's kill (if the boss was a captain, see Captains losing), then resolves the declared intents, dropping any whose captain or target was beaten or whose holding broke up, then declares the next intents.

A declared Push or Challenge appears on the map as a Faction Skirmish inserted into the contested system (an inserted anomaly record from the world, as `atlas.md` planned). If the player runs it before the next turn, the skirmish's existing side choice decides it. If not, it resolves abstractly on the next turn: a strength roll from both captains' rank, holding size and any Defend. A won push moves the system; a won challenge also moves ranks.

Completing a system inside a holding removes it from the holding, since the player cleared its garrison, and captains may claim it again on later turns. So holdings shrink where the player plays and refill from their neighbours, and the war follows the frontier.

The turn is a pure function of the saved state, the seed and a turn counter in `AtlasState`, so it is deterministic and unit-testable. A cap on events per turn keeps the news readable. The turn's results (who beat whom, rank changes, deaths, new and fled captains) are saved in `AtlasState` until the next turn, for the board to show (see Captains board).

## Faction Skirmish

Skirmishes move from generator rolls on territory borders to the world's inserted records on holding borders. The attacker's commander is the attacking captain, or their second-in-command, and the defender's station guards the contested system.

- Siding with the attacker and winning moves the system and counts as the attacker winning the push or challenge.
- Siding with the defender and killing the attacking captain counts as the player defeating that captain (reward choice, scar, flight).
- Staying out resolves the push on its roll when the run closes, whatever the player destroyed.

## Captains losing

When the player kills a captain's flagship as the boss objective, the captain escapes (the officer gets out, as vanilla officers do), drops a rank, gains a scar and loses their seat. Their holding breaks up: its systems become unclaimed, and neighbouring captains can claim them over the next turns.

The fled captain then goes to another holding of their faction on the visible atlas as its second-in-command, if one has room. Otherwise they wait, and take the next new holding of their faction that appears, which the faction weighting above makes likely to be soon. The atlas tooltip and captain intel say where they went, or "beyond the frontier" while they wait.

Fleeing can't outrun the player. A fled captain's new system must be within a few lanes (placeholder 3) of a system the player can reach: a holding further away doesn't count as having room, and a new holding only takes a waiting captain if its seat is that close. New holdings seat as space is revealed, which happens next to the player's progress, so a waiting captain turns up just ahead of the player. A captain whose seat a rival takes flees under the same rule.

A captain beaten by the player at the lowest rank stays at it and flees (see Open questions). Only a lost challenge kills a lowest-rank captain.

## Reward choice

After the player defeats a captain, a prompt offers three items. The player picks one, and the captain carries it: the next time the player defeats that captain, it drops in addition to their usual boss loot. Each defeat drops everything the captain carries and then offers a new choice. A player who loses, retreats or leaves without the kill gets no choice.

A captain who wins a challenge takes every item the loser carries, and carried items stack, so they gather on the captains who keep winning. The player can steer an item onto a captain they want to chase by helping that captain win.

The three offers are drawn without repeats from one pool, weighted by each item's drop chance:

- hull item base types (the nine non-unique `hull_items.csv` rows, by `rarity`), dropping as a normal rolled hull item at the run tier;
- crafting items (by their `chart_materials.csv` share at the run tier);
- rarely, a unique hull item or subroutine (`unique` true, by `rarity`), leaning towards uniques whose `source` is the captain's faction.

The tables use different scales, so each category gets a share of the pool in a small table (placeholders) before the item weights apply within it.

Each option shows its icon, name and normal item tooltip, and the captain's tooltip on the atlas shows what they carry, so the player can pick a target to chase. The pending choice is saved on the captain record until answered, so a reload re-opens the prompt. The prompt opens once the post-battle dialog has closed, as a full-screen picker over the campaign (core `CampaignOverlay`).

## Captains board

A Captains button at the bottom right of the atlas screen opens the captains board: one flowchart of every live captain, never split by faction. Ranks are rows, Admiral at the top, so the board narrows towards the few most powerful captains. Within a row, captains sort by strength (rank roll and holding size). Each captain is their portrait, picked from their faction's vanilla portrait set, with their faction's flag (`FactionAPI.getLogo`) in miniature at its bottom right; everything else is in the tooltip. Fled captains waiting for a holding sit in their rank row with a dimmed portrait.

Arrows between portraits show the war. Plans to claim unclaimed space never show on the board, only on the atlas. Solid arrows run from each winner to the captain they beat on the last turn. Dashed arrows run from each captain to the target of their declared Push or Challenge for the next turn, and a shield mark on a target shows it has declared Defend. Hovering a portrait brightens its own arrows and fades the rest.

The first time the board opens after a turn, it plays a four-second shuffle: portraits start where they stood before the turn and slide to their new places, with a rising mark on those who ranked up, a falling mark on those who ranked down, a fade to nothing for those who died and a fade in for new captains. A Replay button plays it again until the next turn. The pre-turn positions come from the saved turn results, and a seen-turn counter in `AtlasState` makes it play once.

Hovering a portrait shows the captain's faction flag at full size, then their name, rank, faction, trait, scars, carried items and recent history, then their fleet as hovering a fleet in the campaign shows it (fleet name, faction, ship list). The fleet is built from the captain record at the current tier without being spawned, and cached until the record changes, so hovering never rebuilds it. Clicking an identified captain's portrait closes the board and pans the atlas camera to the system they are in (their seat, or their system as a second-in-command); it does nothing for a captain beyond the frontier.

A captain starts unidentified. Unidentified captains show as sensor ghosts on the board: the skill tree's dancing fleet triangles in place of a portrait, with no tooltip, arrows or click. A portrait-sized ghost needs more triangles than the skill tree's three (placeholder 7). `SkillTreeNodeGhostRenderer` moves to core `exiledsector.ui.decoration` with the triangle count, size and scatter as parameters, so the skill tree and the board share it. The shuffle moves ghosts too, so the player sees an unknown captain rise or fall. A captain becomes identified when the player stumbles on them, by bringing their system into view on the atlas or fighting them in any system, or when an anomaly reveals them (see Open questions). Identified is saved on the captain record and never lost.

The system an identified captain is in is always visible on the atlas, even outside normal visibility (2 lanes from a completed system, 6 around a Sensor Array), and follows the captain when they move. Only that system shows: the systems and lanes around it stay fogged until the player's own progress reveals them, and the usual reachability rule still applies. This is how the player sees where a beaten captain went.

## Removing Powerful Boss

Captains replace the Powerful Boss anomaly, so it is deleted as part of this work: `charts.anomaly.PowerfulBoss`, its registry entry in `ChartAnomalies`, its `chart_anomalies.csv` and `atlas_anomaly_chances.csv` rows, the `powerful_boss` case in `AtlasSystemArt`, its strings in both catalogues, its icon, `PowerfulBossTest` and its rows in the other tests, its sections in `docs/docs.md`, `atlas.md`, `anomalies/overview.md` and `atlas_tree_anomalies.md`. Saved anomaly records naming it are dropped on load. Its `guaranteesBossTierUp` loot hook goes too: captains don't guarantee a tier-up chart, since bosses already drop plenty of charts. The Flagship Orders catalyst, which targets it, is deleted as well: its `special_items.csv` and `atlas_catalysts.csv` rows and strings.

## Scars

Each time a captain loses to the player, their flagship's damage tally picks a scar. A damage listener on the boss flagship sums damage taken by theme tag (ballistic, energy, missile, fighters, phase and so on) and the largest share wins. The scar adapts the captain against it: weight multipliers on their NPC tree's focus (on top of `npc_faction_focus.csv`) and refit leanings for the flagship (for example more point defence after losing to missiles). Repeat scars of one tag stack. The scar shows as an epithet and a tooltip line. Scar rows live in a new `atlas_captain_scars.csv`.

## Save footprint

`AtlasState` gains the captain records (capped), the holding claims (packed cell to holding id, a few hundred entries), the turn counter, the declared intents, the last turn's results, the seen-turn counter and the seat decision per revealed chunk. Holding claims replace the capture records; old saves drop their captures (only our saves matter before 1.0).

## Code layout

A new stage package `exiledsector.charts.captain` (added to the CLAUDE.md stage list) holds the records, holdings, turn engine, ranks, scars, identification and reward pool. The holding owner reader and seat placement sit in `charts.atlas`, since they replace `AtlasTerritories`. The reward picker and the captains board go in `exiledsector.ui.atlas`; the shared sensor ghost renderer in core `exiledsector.ui.decoration`. New CSVs go into the editor serialisers.

## Decisions

Decided 2026-11-12:

- Captains are bound to a faction and fight other factions' captains for atlas systems.
- A turn is a boss defeat, never a run closing.
- Rewards are a choice of three, weighted by drop chance, from hull item base types, crafting items and rarely a unique. The chosen item drops on the captain's next defeat on top of their usual loot. No choice without the kill. There is no reward for revenge or for repeat losses.
- Factions are never wiped out: new holdings keep appearing, weighted towards factions missing from the visible atlas, so recently beaten captains can be chased.
- Holdings start at 4 to 10 systems with no size limit after that, growing and shrinking by captures every turn.
- Unclaimed space is independents and mercenaries.
- A captain who wins a challenge takes the loser's carried items.
- A lowest-rank captain who loses a challenge dies for good and is replaced by a new lowest-rank captain.
- A captain beaten by the player drops a rank.
- Five ranks.
- Carried items stack.
- Powerful Boss and the Flagship Orders catalyst are deleted; captains replace them and never guarantee a tier-up chart.
- A captains board opens from the bottom right of the atlas screen: one flowchart of all captains, not split by faction, with ranks as influence and only a few captains at the top ranks. Five military ranks, Lieutenant to Admiral.
- Each captain on the board is their faction portrait with a miniature faction flag at the bottom right; the tooltip shows the flag and holds every detail and the campaign-style fleet list. Clicking a portrait pans the atlas camera to their system.
- The board shows who beat whom on the last turn and who intends to attack or defend against whom on the next, so captains declare their intents a turn ahead. Plans to claim unclaimed space only show on the atlas.
- The first board opening after a turn plays a four-second shuffle of rank changes, deaths and new captains, with a Replay button.
- Unidentified captains are real sensor ghosts (dancing fleet triangles), with more triangles than the skill tree's.
- An identified captain's system is always visible on the atlas; its surroundings stay fogged.
- Captains never move on a turn; they change system only when they lose their seat, and only to somewhere close to what the player can reach, so the player can always catch them.

## Open questions

1. Names: "captain", "holding" and "seat" are placeholders. "Captain" may clash with vanilla's use for a ship's officer, and now with the Captain rank too; "officer" is the alternative for the general word.
2. A captain beaten by the player at the lowest rank: flees at that rank (leaning, so every captain the player meets gets at least one chase), or dies?
3. Which anomaly reveals captains? Leaning the Depot (`listening_post`) and Relay Network, revealing the captains of nearby holdings when the player clears them.
4. A captain who drives the player off (retreat or loss): does it gain anything, given turns only tick on boss kills? Leaning nothing for now.
5. When does a holding get a second-in-command, and do they get their own reward choice?
6. Crafting item offers: one item, or a stack sized like one boss's expected drop at the tier?
7. Can the player skip the prompt, or must they pick?
8. Seat chance per chunk, seat spacing, holdings per faction cap, live captain cap, rank caps, flight distance, starting rank weights, the expansion brake, the absence weights and the category shares are placeholders to set.

## Build order

1. [ ] Holdings: seat positions per chunk, seat faction decided at reveal with the absence weighting, starting claims, the unclaimed faction, the owner reader replacing `AtlasTerritories` and the capture records, borders and fleet traffic through it, and dev console commands to list, claim and release. Tests for determinism, claim connectivity and the faction weighting.
2. [ ] Captain records: generation, the seat boss as the captain, rank effects (`atlas_captain_ranks.csv`), tooltips, intel and the comms greeting with history. Delete Powerful Boss (see Removing Powerful Boss) in the same step, since seats take over its bigger boss.
3. [ ] The turn engine on boss defeat: intents declared a turn ahead (expand, push, challenge, defend, hold); abstract resolution; ranks and rank caps; caps and safeguards; saved turn results. Tests that a turn is a pure function of state, seed and counter.
4. [ ] Captains board: the bottom-right button, rank rows of portraits, the full tooltip with the cached unspawned fleet, last-turn and next-turn arrows, the four-second shuffle and Replay, click to pan the atlas camera, identification on view, fights and anomalies, the shared sensor ghost renderer moved to core, and identified captains' systems always visible on the atlas.
5. [ ] Faction Skirmish on holdings: pushes and challenges inserted as skirmishes, the attacking captain as commander, outcomes feeding the turn engine.
6. [ ] Captains losing: escape, holding break-up, moving to another holding or waiting, and the chase through new holdings.
7. [ ] Reward choice: the weighted pool, the full-screen picker, the pending choice on the record, the carried drop on the next defeat, the atlas tooltip line.
8. [ ] Scars: the damage tally by theme tag, `atlas_captain_scars.csv`, tree focus and refit leanings, epithets.
9. [ ] Strings (en and zh_CN), the `docs/docs.md` section, the editor serialisers for the new CSVs.
