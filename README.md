<h1 align="center">Exiled Sector</h1>

<p align="center"><i>Still sane, exile?</i></p>

<p align="center">
<a href="https://trilink.wispborne.com/open.html?mod=%7B%22url%22%3A%22https%3A%2F%2Fraw.githubusercontent.com%2FExiledPortals%2FExiledSector%2Fmain%2FExiledSector.version%22%2C%22id%22%3A%22exiledSector%22%2C%22version%22%3A%220.1.0%22%7D&dep=%7B%22url%22%3A%22https%3A%2F%2Fraw.githubusercontent.com%2FLazyWizard%2Flazylib%2Fmaster%2Fmod%2Flazylib.version%22%2C%22id%22%3A%22lw_lazylib%22%2C%22version%22%3A%223.0.0%22%7D&dep=%7B%22url%22%3A%22https%3A%2F%2Fraw.githubusercontent.com%2FMagicLibStarsector%2FMagicLib%2Fmaster%2Fmagiclib.version%22%2C%22id%22%3A%22MagicLib%22%2C%22version%22%3A%221.5.6%22%7D&dep=%7B%22url%22%3A%22https%3A%2F%2Fraw.githubusercontent.com%2FLukas22041%2FLunaLib%2Fmain%2FLunaLib.version%22%2C%22id%22%3A%22lunalib%22%2C%22version%22%3A%222.0.5%22%7D"><img src="https://trilink.wispborne.com/badges/install-badge.svg" alt="Install with TriOS"></a>
</p>

<p align="center"><b><a href="https://github.com/ExiledPortals/ExiledSector/releases/latest/download/ExiledSector.zip">Manual download</a></b> | <a href="https://github.com/ExiledPortals/ExiledSector">Source & Changelog (GitHub)</a></p>

## Features

Exiled Sector adds three major modules: a skill tree for every ship, hull upgrades (equipment) for ships, and star charts to go and find more of both.

### The skill tree

Press the Skill Tree button in the refit screen and every ship in your fleet gets a tree of its own, over 300 nodes of armour, flux, guns and questionable engineering decisions. Ships level up in combat and every level buys a node, so your veteran Lasher eventually stops being a Lasher and starts being a problem.

<img src="graphics/description/skill_tree.gif" alt="The skill tree">

You might have to travel a bit for the juicy nodes. Slot a Wormhole Anchor into a Subroutine Socket and it tears a hole out past the edge of the tree, to where the sensible nodes stop. Ctrl+click the socket and in you go.

<img src="graphics/description/wormhole_anchor.gif" alt="Travelling through a Wormhole Anchor">

Every major faction has a volume of its own, full of nodes that think the way it does. The Hegemony thickens your armour, Tri-Tachyon invoices you for it, and the Luddic Path would quite like your ship to explode, ideally next to someone else's.

<img src="graphics/description/faction_volumes.png" alt="Faction volumes">

Doctrine Archives are faction playbooks your crew definitely didn't steal. Some rewrite every node around them into faction-related bonuses, others bolt the doctrine on top. Your engineers read the Tri-Tachyon one and now call the reactor a "growth opportunity".

<img src="graphics/description/doctrine_archive.png" alt="A Doctrine Archive">

Running Random Assortment of Things, Nightcross Armory or LOST_SECTOR? Explore them to find more ways to blow up your enemies.

<img src="graphics/description/mod_volumes.png" alt="Volumes from other mods">

### Hull upgrades

Nobody has designed a new hull since the Collapse, and the Domain's blueprints sit behind DRM older than your grandmother's grandmother. So your crew looks at other people's ships, decides they had some good ideas, and welds those ideas onto yours. 

From level 15, Hull Upgrades turn up in wrecks and salvage. Install one, choose a section of the ship, and that section takes hull items of its own.

<img src="graphics/description/hull_upgrades.gif" alt="Installing a Hull Upgrade and unlocking a section">

Hull items come common, rare and unique, rolling prefixes and suffixes the way the Karui ancestors intended.

<img src="graphics/description/hull_item_1.png" alt="A hull item">
<img src="graphics/description/hull_item_2.png" alt="A hull item">
<img src="graphics/description/hull_item_3.png" alt="A hull item">
<img src="graphics/description/hull_item_4.png" alt="A hull item">

Don't like the rolls? Grow nanoswarms from Nanite Feedstock at the crafting bench and set them loose on your items to add, reroll, reshape or freeze their modifiers. Or corrupt one and find out. Results may vary. Results will vary.

<img src="graphics/description/crafting_bench.gif" alt="The crafting bench">

Note: no Path of Building, but the codex should give you what you need.

The loot turns up everywhere you'd expect it to: research stations, orbital labs, derelicts and the smoking remains of anyone who disagreed with you. The crew gets nothing, as is tradition.

<img src="graphics/description/loot.gif" alt="Looting a research station">

### Star charts

Star Charts lead to systems no map shows, cut off from hyperspace and guarded by fleets that weren't expecting company. They come in 15 tiers of increasingly poor life choices and turn up on tough enemy fleets and inside other charts, so every run you survive hands you an excuse for a worse one.

To open one you need a Wormhole Gateway, a colony structure for people who have run out of normal places to kill their crew.

<img src="graphics/description/wormhole_gateway.png" alt="The Wormhole Gateway">

Charts roll modifiers too, and the same nanoswarms work on them. Bigger fleets, nastier bosses, better rewards. You asked for this (I think? Maybe it was me that asked for this).

<img src="graphics/description/chart_crafting.gif" alt="Crafting a Star Chart">

Open the wormhole, fly in, deal with whoever is home and take everything that isn't bolted down. When the run ends the system goes with it, along with anything you forgot to pick up. No it will not bloat your save (I hope).

<img src="graphics/description/chart_opening.gif" alt="Opening a Star Chart">

## Requirements

- Starsector 0.98a-RC8
- [LazyLib](https://fractalsoftworks.com/forum/index.php?topic=5444.0)
- [MagicLib](https://fractalsoftworks.com/forum/index.php?topic=25868.0)
- [LunaLib](https://fractalsoftworks.com/forum/index.php?topic=25658.0)

Optional:

- Console Commands adds `ExiledSectorGrantFleetXp [amount]`.
- A healthy disregard for vanilla balance.

## Compatibility

> [!WARNING]
> Exiled Sector adds permanent hull mods to the ships in your fleet. It is not safe to remove after being used.

It should be safe to add to an existing save.

Nodes that copy a vanilla hull mod also place the real hull mod on the ship as a phantom: it costs no OP and does nothing itself, but other mods that check for that hull mod see it. That should make most mods work with the tree's nodes without needing dedicated support.

These mods have dedicated compatibility. The version listed is the one I last tested against. Newer versions usually work; if one doesn't, `starsector.log` says so at startup and lists anything Exiled Sector can no longer find.

| Mod | Tested version | What Exiled Sector does with it |
|---|---|---|
| [Second-in-Command](https://fractalsoftworks.com/forum/index.php?topic=30407.0) | 2.0.0 | Its skills see the hull mods your nodes stand in for, and Reconfiguration waives the Converted Hangar penalties. |
| Lost Sector | 0.6.2d, 1.0.b | REDACTED |
| [MagicLib](https://fractalsoftworks.com/forum/index.php?topic=25868.0) | 1.5.6 | Required. A hull mod that tries to strip one of your nodes' hull mods through MagicLib is removed instead. |
| Exotica Technologies | 1.8.7 | The Skill Tree button moves one slot down on the refit screen so it doesn't cover the Exotica button. |
| Random Assortment of Things | 3.3.1 | REDACTED |
| Nightcross Armory | 2.1.6 | REDACTED |
| [GraphicsLib](https://fractalsoftworks.com/forum/index.php?topic=10982.0) | 1.12.1 | Optional. When its lights and distortion are on, Exiled Sector's combat effects light up: Ludd's Light and Phase Anchor send out shockwaves, Frozen Heart stacks pulse, Ion Bolter arcs, Corrosion Beam motes and redirected Hullbreacher and Refracting shots glow, Repair Coordinator drones throw light as they weld, and Candle's Grace Replica bends space around the ship. The Exiled Sector settings can switch all of this off except the mote glow, which GraphicsLib draws itself. |

Some weapons from other mods misbehave (often hilariously) when their beams are split, their shots are chained or their shells pierce. These are listed in:

- `data/config/exiledSector/split_beam_effect_blocklist.csv`
- `data/config/exiledSector/energy_chain_blocklist.csv`
- `data/config/exiledSector/ballistic_pierce_blocklist.csv`

All three files are merged across mods, so other mods can opt their own weapons out (or you can opt them back in, at your own risk).

## FAQ

<details>
<summary><b>How do I earn and spend points?</b></summary>

Open a ship's refit screen and click the **Skill Tree** button under the hull mods, or press **K**. If you prefer the old LunaLib Additional Options entry, switch the button off in LunaLib settings.

The first time you open a ship's tree you choose where it starts: Low Tech, Midline or High Tech. The starting node is free. Whenever it is the only node allocated, you can click it again to choose a different start. The other two can still be reached later like any other node.

Every node costs the ship's unused ordnance points: 1 for frigates, 2 for destroyers, 3 for cruisers and 5 for capitals. A node can only be allocated next to one you already have. Node tooltips show only what a node does. Press **F1** while hovering one to also see its restrictions, such as the hull mods and nodes it is mutually exclusive with.

After every engagement, each ship in your fleet gains XP equal to the deployment points of the enemy ships destroyed or disabled, whether it deployed or not. Harder fights pay more: the same battle difficulty bonus vanilla shows before combat ("Additional XP due to overall battle difficulty") multiplies it, up to 6×. Easier fights never reduce it. You get half as much if you lose.

Each level makes your most recently bought node free and refunds its OP. If every node you have is already free, the level is banked, and your next node costs nothing. The first level takes 60 XP, each level after that takes 13% more until level 25, and ships cap at level 50.

Ships that join your fleet late don't start from scratch. No ship in your fleet sits below your own character level, so a ship bought at level 12 starts at level 12 and keeps pace as you level up. Ships behind your highest-level ship also earn 10% more XP for every level they trail it, up to 4×, so they catch up over time. Both can be changed in LunaLib settings.

Click an allocated node to remove it. OP-paid nodes refund their OP, and free nodes return their free allocation. You can't remove a node that other nodes depend on to connect back to your starting point, and you can't remove the starting point itself.

Ctrl+Shift+click an allocated node to remove it along with every node that depends on it, starting with the farthest. Doing this on your starting point clears the whole tree and lets you choose a new start. For now it does nothing if any of those nodes has its own removal condition (currently any node that adds fighter bays).

For multi-choice nodes, click to open a dropdown so you can pick an option. Ctrl+click repeats your last choice.

Each ship can have a max of 60 nodes.

Every number in this section can be changed in LunaLib settings.

</details>

<details>
<summary><b>What's the difference between "increases" and "more"?</b></summary>

They're two different kinds of modifier, and they stack very differently. If you've played Path of Exile, it's a similar convention.

Starsector calculates a stat like this:

```java
modified = base + base * percentMod / 100 + flatMod;
modified *= mult;
```

| Tooltip wording | Kind | How it stacks |
|---|---|---|
| "10% **increased** / **reduced** flux capacity" | Percent | All percent bonuses on a stat are **added together** first, including those from vanilla hullmods and skills. |
| "**Increases** flux capacity by 600" | Flat | Added after percent bonuses, so percent bonuses don't scale it. This is different to Path of Exile. |
| "10% **more** / **less** flux capacity" | Multiplier | Applied last, to the total. The tree's multipliers are **added together** first (10% more + 10% more = 20% more; 15% more + 30% less = 15% less), and never go past 100% less. They still multiply with multipliers from vanilla hullmods and skills. |

For example, with a base of 1000 flux capacity:

- Two "10% increased flux capacity" nodes give 1000 × (1 + 0.10 + 0.10) = **1200**.
- Two "10% more flux capacity" nodes give 1000 × (1 + 0.10 + 0.10) = **1200**, applied after everything else.
- "15% increased", "Increases by 600", a +10% hullmod and "20% more" together give (1000 + 250 + 600) × 1.2 = **2220**.

Numbers are green when they help your ship and orange when they hurt it.

</details>

<details>
<summary><b>How do NPC fleets get skill trees?</b></summary>

Every fleet can have trees, in every faction, including your own faction's fleets and your allies. Stations don't get them.

Ships with officers and the flagship always get a tree. Each other ship has a 30% chance. Civilian and mothballed ships are left out. The roll is fixed per ship, so reloading a save won't reroll it.

NPC trees mirror your own fleet. Every combat ship you own has a tree power: its level plus the paid nodes it has allocated. Each NPC ship picks one of your combat ships at random and gets 80 to 120% of that ship's tree power in nodes. Free nodes you leave unallocated still count, since the level does, so an empty tree won't make NPCs any easier. Swap your fleet for fresh ships and the fleets you meet after that are sized to the new one. If your combat ships have tree powers of 20 and 40, each NPC ship gets 16 to 24 or 32 to 48 nodes, before the caps below.

The mirror stops at a cap. Pirates never pass 15 nodes, faction fleets (yours and your allies included) 25, and AI fleets 35, so a ship you keep levelling eventually leaves them behind. Bounty targets climb from their cap toward 25 as the bounties get harder, and the target's flagship gets 3 more on top, because a price on your head is motivating. Star Chart fleets don't look at your fleet at all: tier 1 starts at 26 nodes and tier 15 ends at 50. Outside chart systems, a won battle drops charts of the tier its strongest defeated ship would have in a chart, so pirate and faction fleets only ever drop tier 1.

Every NPC captain reads their ship's specs and makes a plan. The plan is usually sensible and occasionally baffling.

They start from the root that matches their hull's design type (Low Tech, Midline or High Tech), or from wherever their academy, priest or AI author happened to teach. Their first job is replacing refit hullmods the tree offers a node for. The OP that frees up buys extra nodes, but if the node is out of reach they keep the hullmod. Built-in hullmods and S-mods are never touched.

Faction captains owe their volume a visit. Ships from a faction with its own volume cross vast expanses to take one notable or keystone there, if they can afford the trip.

After that it's down to taste. A captain picks notables and keystones that suit their weapons, fighters, shields or phase cloak, armour and built-in hullmods, but their choices are often like the sector: random and brutal. Two captains in identical ships will rarely agree. They take the nearest first, pick up small nodes only on the way, and never (theoretically) take a node their ship can't use.

Other mods can map their own factions to a volume in `data/config/exiledSector/npc_faction_volumes.csv`.

NPC trees also hit harder as your fleet grows. Each NPC ship's node bonuses are added up and multiplied, from 1x while your combat ships average a tree power of 15 up to 3x when they average 60. Word gets around. Your average only counts up to that fleet's cap, so the multiplier stops growing there too. Only the bonuses grow: drawbacks stay as written, reductions such as damage taken creep toward their limit without passing it, and the 80% cap on damage taken still holds. Logistics, fleet support and campaign stats such as sensors, cargo and burn level don't scale.

If you recover a ship that has a tree, it keeps its tree, and the tree becomes a normal player tree. Any nodes the NPC bought with OP beyond the ship's level are dropped, and that OP goes back to the ship's fitting. Reinforced Bulkheads' near-guaranteed recovery is switched off on NPC ships, so levelled enemies aren't free loot.

Press **X** (rebindable) during a fleet encounter, or while hovering over a fleet on the map, to see which ships have trees along with their build, level, key nodes and combined bonuses. Pressing **F2** on a ship's tooltip opens a Codex entry with the same summary. In the refit screen, every ship with a tree also shows an **Exiled Sector Skill Tree** hull mod: hover it for the same summary of that ship's bonuses, or click its icon to open the skill tree.

All of this, including the tree size percentages, can be changed on the **NPC Scaling** tab in LunaLib settings.

</details>

<details>
<summary><b>How do I unlock hidden nodes?</b></summary>

Hidden nodes show up as sensor ghosts until you unlock them.

Wormholes unlock when you gain access to the Gate network, the same point vanilla does: using the Janus Device at the end of the Galatia Academy storyline.

Wormholes come in linked pairs. Allocate one end and the other end comes free. It costs nothing extra, but it still counts toward your node limit. Removing either end removes both. Ctrl+click any identified wormhole, allocated or not, to jump the camera to the other end.

Hullmod nodes unlock when you learn the matching hullmod blueprint. Neural Interface also unlocks if you have the relevant Neural Link skill.

If you'd rather skip all of this, LunaLib settings can reveal hidden nodes, or switch off each kind of unlock condition.

</details>

<details>
<summary><b>How does this fit into the lore?</b></summary>

The tree isn't your ship gaining sentience and learning kung fu. It's a picture of you and your crew making lots of small changes that pull the ship away from its factory spec: rerouted conduits, flux grids tuned to the captain's habits, armour patched where it keeps getting hit. That's the same thing vanilla's hullmods represent, just in finer steps. A veteran hull that has survived a dozen campaigns shouldn't fly like one fresh off a Domain-era template, and a ship your crew has been iterating on for years is going to be much more familiar to them. They'll learn the ins and outs of its quirks and get every edge they can.

I may also add a node that teaches your ship kung fu.

Exiled Sector is designed to replace the vanilla hullmod system. I wasn't comfortable disabling hullmods entirely, because that would make the mod parasitic and incompatible with the many mods that add their own. Instead, the OP cost and levelling hybrid is meant to make the tree the better deal most of the time. Every hullmod you install is 3–4 nodes you didn't take. Keep a hullmod when its unique effect, probably from another mod, is worth that trade.

</details>

<details>
<summary><b>Why does every ship have its own tree, instead of one for my character?</b></summary>

Overhauls of the character skill system are a well-explored space, and playing nicely with other mods matters to me, so I didn't want to build something that fights Second-in-Command or the other overhaul mods.

A tree per ship also means that finding a cool new ship, at any point in a playthrough, gives you something new to build up.

One of the things I love most about Starsector is losing myself in designing a ship. Hullmods are the least flexible part of vanilla customisation, and I wanted to replace them with something more interesting.

I also have plans for future features that work much better when each ship has its own tree.

</details>

<details>
<summary><b>What languages is Exiled Sector available in?</b></summary>

Exiled Sector is available in English and Simplified Chinese. By default it follows the game's language, so a Chinese-localised Starsector shows the skill tree in Chinese automatically. You can also pick a language in the LunaLib settings; the change takes effect after a restart.

The skill tree screen always shows the chosen language, using a bundled Noto Sans SC font. Text the game draws itself (settings, dialogs, the codex) stays in English unless the game can display Chinese characters.

The Chinese translation is a first draft awaiting review by a native speaker. To add another language, see the [localisation guide](docs/i18n/README.md).

</details>

## Disclaimer

This mod is in early development.

It is my first mod, and first foray into OpenGL/game development, and is by no means feature complete or bug free.

AI usage: I am a Java software engineer working in the regulatory sector where AI use is heavily restricted at this stage. I started this project as a way to educate myself on the capabilities of LLMs for clean, maintainable code, so that when people asked my opinion I can definitively state that LLMs are not the godsend Sam Altman claims. I still think giving inexperienced devs access to an LLM is like giving an 8 year old a powerdrill. Claude's Opus 5.5 was used for this project.

The art in the mod comes from starsector-core, royalty-free sources, and other mods whose authors kindly gave permission (see Credits).

I am seeking any feedback, bug reports, node suggestions, or faction designs.

You can find me on various Starsector Discords or by direct message @portals_

## Credits

A big thank you to Lukas04 for letting me build the ExoTech and Abyss areas from Random Assortment of Things, to YukiM for the same in Nightcross Armory, and to Kissa Mies and SirHartley for LOST_SECTOR. The loot alert sounds are from Path of Exile and are copyright Grinding Gear Games. [CREDITS.md](CREDITS.md) lists every borrowed file, who made it and the terms it is used under.

## License

Exiled Sector is licensed under the [GNU General Public License v3.0](LICENSE). Fork it, reuse it, change it; just keep your version open source under the same license. This covers the source code only. Art and sounds borrowed from other mods belong to their authors and are not covered by the GPL; ask them before reusing those files.
