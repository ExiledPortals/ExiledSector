# Exiled Sector: mechanics

How some mechanics work when they don't behave the way you'd expect and the game doesn't explain them. Numbers are
placeholders and may change.

Rule for anyone editing this file: it is for players, not developers. Add a section only for something a player would
notice in play, find confusing and come looking for, and that tooltips don't already explain. Write plain prose in
in-game terms, with no class names, file paths, data files or code. Keep each section to one or two short paragraphs,
and describe only what is built.

## Skill tree

### How bonuses stack

Percent bonuses come in two kinds, and the wording tells you which. Increased and reduced bonuses to the same stat add
together before they apply, so 20% increased damage from one node and 20% from another give 40%. More and less bonuses
multiply with that total and with each other, except that copies of the same "more" effect from the tree add together
first, so two nodes with 20% more damage give 40% more.

Weapon bonuses add up across every group a weapon belongs to: an all weapons bonus, an energy weapons bonus and a beam
weapons bonus all add together on a beam. Beam flux cost is the exception, as it multiplies with energy flux cost, so
10% reduced and 10% reduced give 19% less rather than 20%. Damage type bonuses, such as increased kinetic damage, are
separate from weapon damage bonuses and multiply with them.

### Affinity

Low Tech, Midline and High Tech affinity do nothing on their own. They grant bonuses in whole steps: each bonus needs a
full step of 5, 10, 15 or 20 affinity, and anything between steps counts for nothing.

Effects that change affinity apply in a fixed order. First increased Low Tech affinity, then the bonus to your highest
affinity for each s-mod, then raising your lowest affinity to match your highest, and last the exchanges that move your
other two affinities into one. An exchange therefore takes the raised values with it.

### Diminishing returns

Bonuses to armor, hull, flux capacity, flux dissipation, top speed and weapon range from the tree and hull items weaken
once their total passes a threshold: past it, only half of each further bonus counts by default. Thresholds are a
percent of the hull's base value, and flat bonuses count as the percent they add, so +100 armor on a hull with 500
base armor counts as 20%.

Drawbacks lower the total first and are never reduced themselves. Temporary nodes and vanilla hull mods placed by nodes
aren't reduced either. The thresholds and the kept share are LunaLib settings.

### Modules

Ships with modules, and stations, give their modules part of the parent's tree: half of its shared effects by default,
or all of them for stations. Effects that name modules apply to them in full. A ship docked into another ship's module
slot, such as Arma Armatura's core units, keeps its own tree instead.

### Refunded nodes

A node that copies a vanilla hull mod puts the real hull mod on the ship, so other mods see it. A removable hull mod
that conflicts with it is taken off. Some modded ships carry hidden built-in hull mods that remove it instead, and then
the node can't work: it is refunded along with any nodes that depended on it, as a respec would.

The conflict is remembered for every save, so from then on the node can't be taken on any ship with that hull mod.

## Weapons

### Ballistic counts as energy

With this effect, ballistic weapons get every energy weapon bonus from Exiled Sector nodes and hull items, and energy
weapon effects such as Disintegration and refractions work with them too. Energy bonuses from vanilla skills, hull mods
and other mods don't reach ballistic weapons.

### Lion's Gaze

A beam hitting an enemy ship splits to more targets: the nearest enemy ships, not fighters, within half the beam's
range of the impact. The damage is shared evenly between all of them, so the original target takes less, and its EMP
shrinks with it. Integrated Lion's Gaze lets the nearest few split targets take full damage instead of a share.

Each split is a real beam fired by an invisible drone, so the weapon's own effects and your on-hit bonuses apply to it.
A few modded weapons that break when copied use simpler splits without their special effects.

## Items

### Drops

Battle drops scale with the deployment points of the enemy ships you destroy, and past about 3000 DP in one battle
extra DP adds less. Harder battles, by vanilla's battle difficulty, drop up to three times as much, though crafting
items ignore difficulty. Fleets outside Star Charts drop a smaller share than chart fleets, and enemy ships with skill
trees make hull items more likely to have two modifiers.

A hull item that drops has one or two modifiers, never more, so rare items only come from crafting. Unique items are
the exception. Secrets of the Frontier's scripted story fights drop no Exiled Sector loot.

### Unique items after updates

A unique hull item follows its current definition. When an update adds a modifier to a unique, the copies you already
have roll it on load, and they lose modifiers the update took out. Your existing rolls stay the same.

A hull item with a targeting effect picks one other section of the ship as its target. The Extended Warranty Contract
doubles the targeted item's effects, drawbacks included.

### Wormhole anchors

Anchors only drop once you have the Janus Device, unless the LunaLib setting allows them earlier; the Hyper-Nav Beacon
and Omega anchors don't wait for it. An anchor to a story-locked volume only drops once that volume is open:

- Nightcross opens as soon as you find any of its anchors.
- Omega opens on your first win against an Omega fleet, or when you get an Omega anchor, such as the one in the shielded cache at Alpha Site.
- Domain Drones opens on your first win against a Domain-era survey mothership.
- The battles against the Singularity and the Existential Threat each add their volume's anchor to the loot, once per save.

The Shrouded and Omega anchors drop from every won battle against Dweller and Omega fleets. Beating a vanilla deserter
bounty's flagship adds that faction's anchor. If a faction is missing, or Nexerelin has eliminated it, its anchor turns
up in salvage and tech mining sites instead.

## Star Charts

### Starting Star Charts

The Wormhole Tech Blueprint is in the Mothballed Equipment Cache in Limbo. If that cache is already opened or missing,
the blueprint goes straight to your cargo once you have the Janus Device. Right-click it to learn the Wormhole Gateway,
which you build on a colony, and every chart tier opens.

With Vaults of Knowledge, the blueprint is the cost of the gateway research instead. The gateway opens tiers up to 5,
the Gateway Array research upgrades it to tier 10 and the Gateway Nexus to every tier, and your first tier 5 and tier
10 clears each give the next upgrade item. With the No Colony Requirement setting, using the blueprint gives you an
ability that opens wormholes anywhere outside a chart, with every tier open.

### The atlas

Killing a system's boss, its flagship or station, is the only way to complete it, and each completed system gives an
Atlas Skill Point, up to 100. A run that ends without killing the boss uses up the system's anomalies, except star
anomalies, so the next run there is an ordinary system.

### Biome modifiers

Each atlas biome has its own modifier for each hull item section, which drops only there. A non-unique hull item that
drops while you're in a chart of that biome has a 30% chance to get it, replacing a random modifier if that side is
full. Adaptive Reforge and Biome Graft in the Infused Crafting tray add them too, and an item holds at most two.

### Atlas officers

Factions hold territories in the atlas, each run from a command centre by a named officer. Officers have ranks, and
each answers to one officer of the rank above. The atlas takes a turn when you beat an officer, after every fourth
other chart boss, and when an officer's fleet beats you. In a turn, officers push on borders and challenge their
superior or subordinate, and a more renowned officer swaps ranks with their superior. A beaten officer drops a rank,
loses their territory and waits for a new command centre, and a Lieutenant dies on a second loss and is replaced. If
you take a commission, that faction's officers hand their territories to other factions' officers.

Each officer carries items, and beating one drops what they carry and offers three reward groups: your pick is what
they carry next time. Each defeat also gives their flagship a scar matching how your fleet fought it, plus a d-mod, up
to five. Scars make the flagship stronger and raise its level, and the scar it has most of names the officer's
epithet. An officer who loses a challenge to another gains a scar from the winner's faction.

## Chart anomalies

### Relay Network

A Relay Network places a Domain-era nav buoy, comm relay and sensor array. Each one syncs when you do what it listens
for: hold station slowly beside the nav buoy, keep your transponder on near the comm relay, or fire an Active Sensor
Burst near the sensor array. Restoring its power in its dialog, for supplies and heavy machinery, then wakes it. A woken
nav buoy or sensor array gives you vanilla's objective bonus, a woken comm relay shows every fleet in the system, and
each woken relay offers three choices of an item for the boss to drop and a scar for its flagship.

If all three relays wake, the network moves to a nearby system further from the Gateway when the run ends, stronger at
each stage, for up to three stages. The last stage's boss drops a unique hull item or subroutine if all three of its
relays woke.

### Depot

A Depot is a station guarded by three to five pickets in a ring around it, each with extra sensor range. Being seen at
the edge of a picket's sensor range with your transponder off builds its suspicion by 25% a day, which drains 10% a day
once out of contact. Being identified, by coming closer or flying with your transponder on, starts that picket's
half-day report. Full suspicion, a report getting out, or a picket surviving a fight with you raises the alarm, which
never comes down.

After the alarm, the vault loses 20% of its loot a day, down to 25%, and the pickets fly back to guard the depot,
adding their loot to it. Taking the vault before the alarm reveals 3 lanes around the system in the atlas and lets you
choose its prototype hull item from three, instead of getting one.
