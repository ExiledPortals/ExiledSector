# Hull item patterns

Socketing hull items into every ship of a ten-ship wolfpack is tedious. A pattern lets one ship's normal hull items also work on other ships of the same hull type, for Nanite Feedstock.

## Rules

- One pattern per hull type. Hull type is the base hull of the undamaged hull, so D-mod hulls and skins share their base hull's pattern.
- Any ship in the player fleet can become the pattern for its hull type when that hull type has none. Making a pattern is free.
- While a pattern exists, no other ship of that hull type can become one. Its Hull Patterns tab names the pattern ship and where it is, and can delete that pattern.
- The pattern ship selects up to five of its sockets to clone. A socket can be selected only when the pattern ship has it unlocked, meets its requirement and holds no unique there.
- The pattern ship links followers: ships of the same hull type in the player fleet. A pattern ship is never a follower.
- Cost is 500 Nanite Feedstock per selected socket per linked ship. Selecting a socket charges 500 for every linked ship, including linked ships that are away. Linking a ship charges 500 for every selected socket. Deselecting a socket, unlinking a ship and deleting the pattern refund nothing, so reselecting or relinking pays again. Every one of those asks for confirmation. An action the player can't afford is blocked; nothing is charged in part.
- A follower clones a socket's item when all of these hold: the pattern ship is in the player fleet, the socket is selected, the pattern ship has that socket unlocked and meets its requirement, it holds a normal (not unique) item there, the follower has the socket unlocked and meets its requirement, and the follower has no item of its own there.
- Swapping, emptying or locking a socket on the pattern ship, or breaking its requirement, keeps the socket selected and paid. It clones whatever normal item is there once it is usable again.
- Changing an item on the pattern ship changes it on every follower at once.
- A pattern ship moved to storage or another player fleet keeps its pattern, which stops applying until the ship returns to the player fleet. A pattern ship sold or destroyed deletes the pattern; its items go back to storage or are destroyed as they are today.
- A follower that leaves the fleet stays linked and paid. A follower destroyed for good is unlinked, and the pattern ship loses one random item from the sockets that follower was cloning at the time. Nothing is lost when it cloned nothing. The pattern tab warns about this.
- Stations can't be patterns or followers, and ship modules never take part since only player fleet members can.
- With Hull Upgrades switched off, patterns stay saved and do nothing.

## Steps

1. Pattern data, registry, costs and rules. Done: `exiledsector.items.pattern` (`HullPattern`, `HullPatterns`, `HullTypes`, `HullPatternRules`) in exiledsector-hull-upgrades.
2. Cloned items in sockets and their effects. Done: `HullPatternClones` and `ShipSections` (an `UnlockedSection` knows whether its item is cloned), with the pattern state in the sections cache.
3. Pattern activity: in-fleet check refreshed on player fleet sync, followers' stats refreshed when the pattern changes. Done: `HullPatternActivity`, wired in `HullUpgradesDomain`.
4. Custody: deleting a sold or destroyed pattern ship's pattern, unlinking a destroyed follower and the item loss. Done: `HullPatternCustody` (id-aware `ShipCustody` hooks in core).
5. Hull Patterns tab on the hull items screen. Done: `HullPatternPanel` and `PatternEditor`, `SocketPanelTab.HULL_PATTERNS`.
6. Cloned items shown on follower sockets with their source. Done: `SectionsDisplay`.
7. Tests. Done: `HullPatternsTest` in exiledsector-hull-upgrades (cloning, overrides, costs, one pattern per type, stations, uniques, the socket cap, dormancy, returning followers, losses).
