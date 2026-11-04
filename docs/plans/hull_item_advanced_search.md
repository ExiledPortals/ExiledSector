# Hull item advanced search

Status: planned, nothing built.

The storage panel's text search matches words and one "20+" minimum per term. Players with large stashes want a Path of Exile style trade search instead: pick stats from a list, give each a min and max, and combine them in AND and COUNT groups.

## Layout

An Advanced button next to the search field opens a pane over the storage panel. The free-text search stays as the quick option, and both apply together.

From top to bottom:
1. The existing Socket type and Rarity filters, shared with the storage panel, so picking one in either place updates both.
2. The modifier groups, each with its rows and an Add modifier button.
3. Add AND group, Add COUNT group, Reset and Search buttons.

A group's header names its kind. A COUNT group has a number field in its header: at least that many of its rows must match. A row is a stat picker, a lines button, a min field, a max field and a remove button. The lines button cycles Both (the default), Implicit and Explicit on each click. Removing a group's last row removes the group.

## Matching

Search applies the groups. Until it's pressed the storage panel keeps the last applied search. An item passes when it passes every group:
- AND: every row matches.
- COUNT n: at least n rows match. An n above the row count can never pass, and the field doesn't cap it.
- A row with no stat picked is ignored, and so is a group with no stats picked.
- A blank min or max means no limit on that side.

A row matches when the item's total for that stat is between min and max. The total sums every line of that stat on the item, from the lines that row's lines button selects, so 15% from one roll and 5% from a hybrid line make 20%. An item with no line of that stat has no total and fails the row, even with min blank, so a row always means "has this stat".

Totals compare against the value shown on the item, sign included, after rounding to the shown decimals, so "min 20" matches a shown 20. A reduction stat like minimum crew shows negative, so "max -7" means a reduction of 7% or more.

Tier-group lines resolve at the item's tier, as the tooltip does. Ten lines scale with hull size and show every value in storage ("5/10/15/20"). For those the item gets one total per hull size, and the row matches if any hull size's total is in range.

## Stat picker

The picker is a text field over a list: the field filters the list by "contains" on every keystroke, and clicking an entry picks it. Starsector has no combo box, so it's a text field plus a `FilterMenu` style list. It reuses the panel's existing text polling, without the search delay.

One entry per stat (effect name), not per modifier, so hybrids don't add entries. The label is the stat's own line with `#` in place of the number, like "+#% flux dissipation", taken from the effect's description. It isn't new translated text, so it stays in sync with the tooltip wording.

The list holds every stat that can roll on a hull item: implicits and the explicit pools. With socket types chosen, it narrows to the stats those types can roll. A picked stat that the narrowed list no longer holds stays picked and keeps matching.

## Code

All in `exiledsector-core`, `exiledsector.ui.socket` except where noted:
- `ItemStatTotals` (`exiledsector.items`): an item's per-stat totals, implicit and explicit kept apart, with per-hull-size totals for scaled lines. Built once in `ItemStorageRow.of`, not per frame.
- `ModifierSearch`: the groups and rows, each row with its lines choice, plus the matching rules. It lives on `ItemStorageFilter`, so it lasts the session and Reset clears it. `ItemStorageQuery.apply` adds it as one more filter, and `isDefault` counts it.
- `StatPicker` and `AdvancedSearchPane`: the UI.
- The pool of pickable stats per socket type comes from `ItemDefinitions` and `ItemModifiers`, cached on first use.
- New text goes in the core catalogue (`en.json` and `zh_CN.json`), following `docs/tooltip_wording.md`.

## Steps

1. `ItemStatTotals` with tests: hybrid lines summed into each of their stats, the implicit/explicit split, per-hull-size totals and rounding to the shown decimals.
2. `ModifierSearch` with tests: AND, COUNT, blank min and max, each lines choice, ignored empty rows and groups, a stat missing from the item.
3. Stat pool per socket type, with a test that every hull item stat appears in at least one pool.
4. `StatPicker`, filtering on each keystroke.
5. `AdvancedSearchPane` and the Advanced button, wired into `ItemStorageQuery` and Reset.
6. A `docs/docs.md` section if any matching rule isn't evident from play.
