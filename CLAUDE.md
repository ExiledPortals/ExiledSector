# Exiled Sector

Starsector mod (Java 17, Maven) that gives every ship its own skill tree. Mod id `exiledSector`, plugin `exiledsector.ExiledSectorModPlugin`, targets game `0.98a-RC8`. Depends on LazyLib, MagicLib, LunaLib.

## Build and deploy (Windows, PowerShell)
- Build and test: `mvn -q clean package` (writes Exiled Sector's jars into `jars/`, the list in `mod_info.json`, and `ExiledLib.jar` into `exiled-lib/mod/jars/`). If `mvn` isn't on PATH, `build-common.ps1` finds IntelliJ's bundled Maven and sets `JAVA_HOME` to `C:\Program Files\Java\jdk-17`.
- Deploy to the game: `.\deploy.ps1`. It runs only from the dev checkout `E:\Dev\ExiledSector-parallel-work` (normal deploys) and the main checkout `E:\Dev\ExiledSector` (hotfix testing), so other worktrees can't overwrite the deployed mod. It writes two mod folders, `ExiledSector` and `ExiledLib`. Both checkouts share the deploy targets, so each deploy replaces the other's build. Don't work around that check.
- Python is not installed (`python` only opens the Microsoft Store). Don't write Python scripts; use PowerShell, Git Bash tools or the Edit tool.
- `release.ps1` builds the release zip. Don't run it unless asked.
- Tests are JUnit 5 + Mockito under each module's `src/test/java`, run from the repo root. A test sits in the lowest module that sees every class it uses, and each module installs only the domains it can see (`exiledsector.test.domains` in its pom). The surefire `--add-opens` args in the parent `pom.xml` are needed; don't remove them.

## Layout
- Maven modules, one per part in `docs/plans/domain_split_classes.csv`: `exiled-lib`, `exiledsector-core`, `exiledsector-skill-tree`, `exiledsector-hull-upgrades`, `exiledsector-star-charts`, and `exiledsector-mod` (the mod plugin and the tests that need every domain). Exiled Lib is also its own Starsector mod (id `exiledLib`), whose mod files live in `exiled-lib/mod/` and whose classes are in `exiledlib.*` packages. The other modules keep `exiledsector.*` packages, so a package's classes can be spread over several of them. Each feature domain has a LunaLib switch (`enabled()` on its `ExiledSectorDomain`), and core skips a switched-off domain's effect sources, loot sources, summary parts, refit buttons and bench modes, so hook new domain features in through those rather than calling them from core.
- Exiled Lib exists so other Starsector mods can reuse the skill effects in their own implementations. It holds only the effects and what they need to run, and nothing else, even code every Exiled Sector module uses. Anything only Exiled Sector uses (the skill tree, items, UI, panels, tooltip layout, campaign wiring, settings pages) goes in the lowest `exiledsector-*` module that sees all its users, usually `exiledsector-core`. When a lib class needs Exiled Sector code, have the mod pass it in or move that lib class out; never pull the code into lib. `docs/plans/domain_split.md` step 10 moves out what's in lib today against this rule.
- `<module>/src/main/java/exiledsector/` (`exiledlib/` in Exiled Lib): `skills/` (tree, loader, `skilleffect/` has ~94 effect classes), `items/` (hull items, nanoswarms), `charts/` (Star Charts), `ui/` (refit, node, cargo, section), `effects/`, `compat/`, `console/`, `i18n/`, `persistence/`.
- `data/`: game data. Skill tree and skill type definitions are in `data/skilltrees/` (`skill_types.json` holds default magnitudes). Strings are in `data/strings/`.
- `graphics/`: art. `graphics/description/` and `graphics/unused/` are excluded from deploys.
- `docs/docs.md`: how the non-obvious skill effects work. Read the relevant section before changing an effect. `docs/plans/` holds design plans, `docs/i18n/` the translation glossary.
- `libs/` and `jars/` are gitignored; don't edit or commit them.

## Git
- Several worktrees share this repo: `ExiledSector` (main) and `ExiledSector-parallel-work`. Check `git worktree list` and the current branch before committing. `parallel-work` is the dev branch: features land there, and it merges into `main` when it's ready. `main` takes only hotfixes, which get merged into `parallel-work` too. Feature worktrees branch from `parallel-work`, not `main`.
- Don't read or open `hs_err_pid*.log` or `replay_pid*.log` in the repo root. They're JVM crash dumps (gitignored, tens of KB each). If asked about a crash, read only the first ~40 lines.

## Conventions
- User-facing text goes through `exiledlib.i18n` (`I18n.forGameText(() -> Translation.msg("key")...)`), with message keys in the catalogue of the class's part (`exiled-lib/mod/data/strings/exiledLib/` or `data/strings/exiledSector/<part>/`), not hardcoded literals.
- Magnitudes are placeholders pending balance (`docs/analysis/balance.csv`). Don't treat current values as intended.
- Update `docs/docs.md` when you change how an effect behaves. Keep it short: one or two short paragraphs per section at most, less is better. Say what it does and where the code lives, no edge cases or rationale. Shorten an existing section rather than grow it. Describe only what is built, never planned or missing features (those go in `docs/plans/`).
- Every modifier, effect and stat line follows `docs/tooltip_wording.md` (increased/reduced vs more/less by how the value stacks in code, "+N to", "additional", category names, no "loot") and keeps every description short: less is more, never over-explain. Read it before writing or changing one.
- Line endings: git stores LF and `core.autocrlf=true` checks most files out as CRLF (some stay LF). That's expected and git normalises on commit, so don't report CRLF as a problem or convert files wholesale. Only mixed endings inside one file matter: the editor's byte-for-byte round-trip test fails on them. The Edit tool keeps a file's endings. Git Bash `sed -i` rewrites a CRLF file as LF, and scripts that split on `\n` leave stray `\r`, so when scripting an edit, write the file back with the ending it already had.
