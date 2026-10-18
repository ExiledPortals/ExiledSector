# Exiled Sector

Starsector mod (Java 17, Maven) that gives every ship its own skill tree. Mod id `exiledSector`, plugin `exiledsector.ExiledSectorModPlugin`, targets game `0.98a-RC8`. Depends on LazyLib, MagicLib, LunaLib.

## Build and deploy (Windows, PowerShell)
- Build and test: `mvn -q clean package` (writes `jars/ExiledSector.jar`). If `mvn` isn't on PATH, `build-common.ps1` finds IntelliJ's bundled Maven and sets `JAVA_HOME` to `C:\Program Files\Java\jdk-17`.
- Deploy to the game: `.\deploy.ps1`. It refuses to run outside the main checkout `E:\Dev\ExiledSector`, so worktrees can't overwrite the deployed mod. Don't work around that check.
- `release.ps1` builds the release zip. Don't run it unless asked.
- Tests are JUnit 5 + Mockito under `src/test/java`. The surefire `--add-opens` args in `pom.xml` are needed; don't remove them.

## Layout
- `src/main/java/exiledsector/`: `skills/` (tree, loader, `skilleffect/` has ~94 effect classes), `items/` (hull items, nanoswarms), `charts/` (Star Charts), `ui/` (refit, node, cargo, section), `effects/`, `compat/`, `console/`, `i18n/`, `persistence/`.
- `data/`: game data. Skill tree and skill type definitions are in `data/skilltrees/` (`skill_types.json` holds default magnitudes). Strings are in `data/strings/`.
- `graphics/`: art. `graphics/description/` and `graphics/unused/` are excluded from deploys.
- `docs/docs.md`: how the non-obvious skill effects work. Read the relevant section before changing an effect. `docs/plans/` holds design plans, `docs/i18n/` the translation glossary.
- `libs/` and `jars/` are gitignored; don't edit or commit them.

## Git
- Several worktrees share this repo: `ExiledSector` (main), `ExiledSector-parallel-work`, `-nightcross-areas`, `-rat-areas`. Check `git worktree list` and the current branch before committing. Branches are merged back into `main`.
- Don't read or open `hs_err_pid*.log` or `replay_pid*.log` in the repo root. They're JVM crash dumps (gitignored, tens of KB each). If asked about a crash, read only the first ~40 lines.

## Conventions
- User-facing text goes through `exiledsector.i18n` (`I18n.forGameText(() -> Translation.msg("key")...)`), with message keys under `data/strings/exiledSector`, not hardcoded literals.
- Magnitudes are placeholders pending balance (`docs/analysis/balance.csv`). Don't treat current values as intended.
- Update `docs/docs.md` when you change how an effect behaves.
- Line endings: git stores LF and `core.autocrlf=true` checks most files out as CRLF (some stay LF). That's expected and git normalises on commit, so don't report CRLF as a problem or convert files wholesale. Only mixed endings inside one file matter: the editor's byte-for-byte round-trip test fails on them. The Edit tool keeps a file's endings. Git Bash `sed -i` rewrites a CRLF file as LF, and scripts that split on `\n` leave stray `\r`, so when scripting an edit, write the file back with the ending it already had.
