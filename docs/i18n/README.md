# Localisation

All player-visible text lives in message catalogues under `data/strings/exiledSector/`. `en.json` holds
the English source for everything the Java code shows, and `zh_CN.json` holds the Simplified Chinese
translation.

A few kinds of text keep their English source in the data files instead: skill type names and
descriptions in `data/skilltrees/skill_types.json`, and our hull mods' names and descriptions in
`data/hullmods/hull_mods.csv`. Translations refer to these by id, using the keys
`skillType.<id>.name`, `skillType.<id>.description`, `hullmod.<id>.name` and
`hullmod.<id>.description`.

## Choosing the language

The Exiled Sector tab in LunaLib's settings has a Language option with three choices: Auto, English and
Simplified Chinese. Auto follows Starsector's `localeOverride` from `settings.json`. A change takes effect
after restarting the game. Whichever language is chosen, any key without a translation falls back to its
English text.

## Where text is drawn

The skill tree screen is drawn by the mod itself using LazyLib, in Orbitron or in whatever font the
language names with `meta.font`, so it can always show the chosen language.

Everything else is drawn by the game with its own fonts. That includes the LunaLib settings, dialogs, the
codex, combat floating text and hull mod text. The game can only display Chinese characters when its own
locale is Chinese or `cjkMode` is on, so this text switches to Chinese only in those cases and otherwise
stays in English.

## Writing strings

`{name}` placeholders are filled in by the code, and values are inserted exactly as given.

Colour comes from tags placed around words. `<good>…</good>` and `<bad>…</bad>` mark improvements and
drawbacks, and they swap automatically for stats where a lower value is better. `<hl>…</hl>` is a neutral
highlight, and `<hullmod>…</hullmod>` and `<node>…</node>` use the hull mod and node colours. Tags can't be
nested.

A key can have `.one` and `.other` plural forms, and the code picks between them from the count. Languages
without plurals, such as Chinese, only need `.other`. List items are separated with `format.list.sep`, and
appended sentences are joined with `format.sentences`.

Two characters need care. `settings.*` strings must not contain `%`, because LunaLib passes them through
`String.format`. And `#` must never be used anywhere, because Starsector's JSON loader treats it as the
start of a comment.

`CatalogueLintTest` checks every catalogue. Placeholders and tags must match the English, tags must be
balanced, and every key must correspond to an English key or a data id; the only exception is the
`meta.*` settings, which have no English counterpart. The test also writes
`target/i18n/missing-<locale>.txt` listing untranslated strings, and `release.ps1` warns when that file
isn't empty.

## Adding a language from another mod

A translation mod can ship its own `data/strings/exiledSector/<locale>.json`, for example `ru.json` or
`ja.json`, and Starsector merges it into our catalogue. The same mechanism lets a mod override individual
keys of a language that already exists.

If the language needs characters that Orbitron doesn't have, set `meta.font` to a single-page BMFont atlas
and ship that too; LazyLib can't read fonts that span several pages. If the double-drawn bold effect smears
the glyphs, set `meta.fakeBold` to `false`.

The test-scope `BitmapFontGenerator` can build a suitable atlas:

```
java -cp target/test-classes;target/classes exiledsector.i18n.BitmapFontGenerator <font.ttf|.otf> graphics/fonts/exiledSector/<name>.fnt 17 20 2048 1024 data/strings/exiledSector/en.json data/strings/exiledSector/<locale>.json
```

The arguments are, in order: the source font, the output `.fnt` path, the glyph size in pixels, the line
height, the atlas width and height, and the text files whose characters must be included. Keep the line
height at 20, the body text size, because LazyLib scales against it; that way body text draws at the
atlas's own size instead of being scaled down. For the atlas, use the smallest power-of-two size the
glyphs fit into, since every pixel costs four bytes of video memory.

On top of the characters in the given files, the generator always includes ASCII, Latin-1, common
punctuation, CJK symbols, full-width forms and the 3,755 common GB2312 hanzi. If the glyphs don't fit, it
fails rather than silently dropping any.

## Testing for missed strings

Setting `localeOverride` to `en_XA` switches on a pseudo-locale in which every catalogue string appears as
`[text~]`, so any hard-coded English stands out immediately. `PseudoLocaleLeakTest` and
`ExiledSectorSettingsTest` run the same check automatically over every description and setting.
