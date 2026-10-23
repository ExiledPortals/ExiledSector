# Skill effect ideas

Ideas for new skill effects and nodes, not yet designed or built. When one gets a real design, move it to its own plan in `docs/plans/` and link it here. Delete it from this list once it's built.

Each idea notes the skill effect part it would most likely go in (`exiledlib/skills/skilleffect/<part>/`).

## Pull enemies in by ship mass

A node that pulls nearby enemy ships toward this ship, scaled by ship mass: heavier ships pull harder, or lighter targets are pulled further. Part: `aura`.

## Ship mass increases and reductions

Effects that raise or lower ship mass. More mass means it shoves other ships harder in collisions and gets knocked around less. Less mass means it's knocked around more but accelerates faster. Part: `movement`.

## Bombers turn into Terminator drones

A bomber that has fired its last missile behaves like a Terminator drone instead of flying home to rearm. Part: `fighter`.

## Fighters at the start of a battle

For the first 120 seconds of a battle, fighters have unlimited engagement range and more top speed. Part: `fighter`, or `battle` if it reads the battle clock.

## More from Heartless stacks

Today Heartless stacks come only from ships destroyed nearby (`NearbyDestructionHeartless`), and spend on armor, mobility, radiation EMP and Live Munitions (`HeartlessStacks`, `LiveMunitionsListener`, in `special`). Ideas:

- more ways to gain stacks;
- more effects per stack;
- spending stacks when the ship system is used, for a stronger system activation.
