# Exiled Sector

Starsector mod (Java 17, Maven) that gives every ship its own skill tree. Mod id `exiledSector`, plugin `exiledsector.ExiledSectorModPlugin`, targets game `0.98a-RC8`. Depends on LazyLib, MagicLib, LunaLib.

## Build and deploy (Windows, PowerShell)
- Build and test from the repo root: `mvn -q clean package` (also rewrites the jar list in `mod_info.json`). Without `mvn` on PATH, use IntelliJ's bundled Maven with `JAVA_HOME=C:\Program Files\Java\jdk-17`, as `build-common.ps1` does. `jars/`, `exiled-lib/mod/jars/` and `libs/` are generated and gitignored; don't edit them.
- Deploy: `.\deploy.ps1`. It only runs from `E:\Dev\ExiledSector-parallel-work` (normal), `E:\Dev\ExiledSector-new-faction-areas` (volume testing) or `E:\Dev\ExiledSector` (hotfix testing), which write the same `ExiledSector` and `ExiledLib` mod folders. Don't work around that check.
- Don't run `release.ps1` unless asked. Python isn't installed (`python` opens the Microsoft Store); use PowerShell, Git Bash tools or Edit.
- Tests: JUnit 5 + Mockito in each module's `src/test/java`. A test sits in the lowest module that sees every class it uses; each module installs only the domains it can see (`exiledsector.test.domains` in its pom). Keep the surefire `--add-opens` args in the parent `pom.xml`.

## Layout
- Modules: `exiled-lib`, `exiledsector-core`, `exiledsector-skill-tree`, `exiledsector-hull-upgrades`, `exiledsector-star-charts`, `exiledsector-mod` (plugin plus tests needing every domain). Exiled Lib is also its own mod (id `exiledLib`, files in `exiled-lib/mod/`, packages `exiledlib.*`); the rest use `exiledsector.*`, so one package can span modules.
- Exiled Lib lets other mods reuse the skill effects: it holds the effects and what another mod needs to apply, stack and describe them (`EffectTotals`, caps, scaling). Anything a reusing mod wouldn't call (tree, items and item framework, UI and UI helpers, tooltip layout, campaign wiring, settings, `ExiledSectorDomain` hooks) goes in the lowest `exiledsector-*` module that sees all its users, usually core. When lib needs mod code, have the mod pass it in or move the lib class out; never pull mod code into lib.
- Each feature domain has a LunaLib switch (core `DomainSwitch`, read by `ExiledSectorDomain.enabled()`). Hook domain features in through the domain hooks, not from core. Content hooks (effect sources, loot sources, summary parts, fit effects, ship descriptions, refit buttons, bench modes) skip switched-off domains via `DomainContributions` or `ExiledSector.enabledDomains()`. State hooks (lifecycle and save repair, custody, ship records, member and fleet sync, engagement steps, item rules and icons) run for every domain and check the switch inside, since they also clean up after a switch-off.
- Skill effects (`exiledlib/skills/skilleffect/`): the root package is the framework; each subpackage is one ship part as Starsector shows it, with one `<Part>SkillEffect` enum and the listeners only that part uses (`render` holds drawing helpers). A plain stat effect goes in its stat's part, a situational one in that situation's part (phased, flux level, on hit, nearby ships, station modules, low hull). `special` is only for named mechanics spanning several parts or changing death or AI behaviour. Register new enums in `SkillEffectRegistry`. Tests mirror these packages.
- Star Charts classes go in the `exiledsector.charts` subpackage for their stage (run, run.cleanup, wormhole, generation, layout, content, objective, effect, loot, anomaly, atlas); the root holds only wiring and shared ids, and `charts.table` the shared CSV table helper. Tests mirror these packages.
- Skill trees and skill types are in `data/skilltrees/` (`skill_types.json` holds default magnitudes). `graphics/description/` and `graphics/unused/` aren't deployed.
- `docs/docs.md` explains the non-obvious effects; read the relevant section before changing one. `docs/plans/` holds design plans (move a fully built one into `docs/plans/completed/`), `docs/i18n/` the translation glossary.

## Git
- Worktrees share this repo; check `git worktree list` and the branch before committing. `parallel-work` is the dev branch and merges into `main` when ready; `main` takes only hotfixes, which also merge into `parallel-work`. Feature worktrees branch from `parallel-work`.
- Each faction volume (e.g. `tania-redesign`, `iron_shell_volume`) lives on its own branch until its mod author approves it, worked on by one session in its own worktree. Volume branches are cut from and updated only from `parallel-work`.
- `new-faction-areas` (`E:\Dev\ExiledSector-new-faction-areas`) deploys and tests one volume at a time on top of regular `parallel-work` merges. Only merges and conflict fixes happen there; volume changes go on the volume's branch. It merges into `parallel-work` once the author approves and the user says so.
- A volume session may commit finished work (builds, tests pass) on its own branch without asking; the `new-faction-areas` session may commit its merges. Neither pushes or merges into `parallel-work` or `main` without the user.
- Don't open `hs_err_pid*.log` or `replay_pid*.log` in the repo root (JVM crash dumps); for a crash, read only the first ~40 lines.

## Conventions
- User-facing text goes through `exiledlib.i18n` (`I18n.forGameText(() -> Translation.msg("key")...)`), keys in the catalogue of the class's part (`exiled-lib/mod/data/strings/exiledLib/` or `data/strings/exiledSector/<part>/`), never literals.
- Magnitudes are placeholders pending balance (`docs/analysis/balance.csv`); don't treat them as intended.
- When an effect's behaviour changes, update `docs/docs.md`: at most two short paragraphs per section, saying what it does and where the code lives. No edge cases, rationale or unbuilt features (those go in `docs/plans/`). Shorten sections rather than grow them.
- Read `docs/tooltip_wording.md` before writing or changing any modifier, effect or stat line, and follow it. Keep descriptions short.
- Line endings: git stores LF and checks most files out as CRLF; that's expected, don't report or convert it. Only mixed endings within one file matter (the editor round-trip test fails on them). Edit keeps endings; `sed -i` and scripts splitting on `\n` don't, so write scripted edits back with the file's existing ending.
