package exiledsector.socketables;

import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.skilleffect.EffectAliases;
import org.json.CDL;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReleasedSocketablesTest {

    private static final Path LEDGER = Path.of("src/test/resources/released_socketables.csv");
    private static final String LEDGER_HEADER = "definition,effect,firstRelease,retiredAfter";

    record ReleasedPoolEffect(String definitionId, String effectName, String firstRelease, String retiredAfter) {

        boolean retired() {
            return !retiredAfter.isEmpty();
        }

        String currentEffectName() {
            return new RolledEffect(effectName, 0f).effectName();
        }

        @Override
        public String toString() {
            return definitionId + ":" + effectName;
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        EffectAliases.register(csvRows(EffectAliases.DATA_PATH));
        SocketableDefinitions.register(csvRows(SocketableDefinitions.DATA_PATH));
        SocketableNames.registerAffixes(csvRows(SocketableNames.AFFIXES_PATH));
    }

    @AfterEach
    void tearDown() {
        SocketableNames.clear();
        SocketableDefinitions.clear();
        EffectAliases.clear();
    }

    private static JSONArray csvRows(String dataPath) throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(dataPath), StandardCharsets.UTF_8);
        return CDL.toJSONArray(csv.replace("\r\n", "\n"));
    }

    private static List<ReleasedPoolEffect> ledger() throws IOException {
        List<String> lines = Files.readAllLines(RealSkillData.projectRoot().resolve(LEDGER), StandardCharsets.UTF_8);
        assertEquals(LEDGER_HEADER, lines.get(0).trim());
        List<ReleasedPoolEffect> released = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) {
            if (line.isBlank()) {
                continue;
            }
            String[] fields = line.split(",", -1);
            assertEquals(4, fields.length, "malformed ledger line: " + line);
            released.add(new ReleasedPoolEffect(fields[0].trim(), fields[1].trim(), fields[2].trim(), fields[3].trim()));
        }
        return released;
    }

    @Test
    void theLedgerListsEachReleasedPoolEffectOnceWithItsFirstRelease() throws Exception {
        List<ReleasedPoolEffect> released = ledger();
        Set<String> seen = new HashSet<>();
        List<ReleasedPoolEffect> malformed = released.stream()
                .filter(entry -> entry.definitionId().isEmpty() || entry.effectName().isEmpty() || entry.firstRelease().isEmpty()
                        || !seen.add(entry.toString()))
                .toList();

        assertFalse(released.isEmpty());
        assertEquals(List.of(), malformed, "These rows of " + LEDGER + " are blank or listed twice.");
    }

    @Test
    void everyDefinitionThatShippedInAReleaseStillLoads() throws Exception {
        Set<String> releasedDefinitions = new LinkedHashSet<>();
        ledger().forEach(entry -> releasedDefinitions.add(entry.definitionId()));
        List<String> missing = releasedDefinitions.stream().filter(id -> SocketableDefinitions.get(id) == null).toList();

        assertEquals(List.of(), missing, "These socketable ids shipped in a release but no longer load from " + SocketableDefinitions.DATA_PATH
                + ". Every owned copy shows as Unknown, applies with no pool rules, disassembles as a common and NPC ships carrying"
                + " them stop dropping them. Restore the row or fix its parse error (see the log).");
    }

    @Test
    void everyPoolEffectThatShippedInAReleaseIsStillASkillEffect() throws Exception {
        List<ReleasedPoolEffect> gone = ledger().stream().filter(entry -> new RolledEffect(entry.effectName(), 0f).effect() == null).toList();

        assertEquals(List.of(), gone, "These pool effects shipped in a release but are no longer skill effects. Items that rolled them"
                + " silently lose the bonus. Keep the effect, or add a row to " + EffectAliases.DATA_PATH + " naming its replacement.");
    }

    @Test
    void everyPoolEffectThatShippedInAReleaseIsStillInItsPoolUnlessRetired() throws Exception {
        List<ReleasedPoolEffect> dropped = ledger().stream()
                .filter(entry -> !entry.retired())
                .filter(entry -> SocketableDefinitions.get(entry.definitionId()) != null)
                .filter(entry -> SocketableDefinitions.get(entry.definitionId()).poolEntry(entry.currentEffectName()) == null)
                .toList();

        assertEquals(List.of(), dropped, "These pool effects shipped in a release but are no longer in their definition's pool in "
                + SocketableDefinitions.DATA_PATH + ". Items that rolled them keep their stored value but lose its roll range, cannot"
                + " recalibrate it and only guess its prefix or suffix side when augmented. If the effect was renamed, add a row to "
                + EffectAliases.DATA_PATH + "; if it left the pool on purpose, set retiredAfter to the last release that had it in "
                + LEDGER + ".");
    }

    @Test
    void retiredPoolEffectsAreReallyOutOfThePool() throws Exception {
        List<ReleasedPoolEffect> stale = ledger().stream()
                .filter(ReleasedPoolEffect::retired)
                .filter(entry -> SocketableDefinitions.get(entry.definitionId()) != null)
                .filter(entry -> SocketableDefinitions.get(entry.definitionId()).poolEntry(entry.currentEffectName()) != null)
                .toList();

        assertEquals(List.of(), stale, "These pool effects are marked retired in " + LEDGER + " but are back in their pool."
                + " Clear their retiredAfter column.");
    }

    @Test
    void everyReleasedEffectOnARollingDefinitionKeepsTheAffixItsFrozenNamesUse() throws Exception {
        List<ReleasedPoolEffect> nameless = ledger().stream()
                .filter(entry -> SocketableDefinitions.get(entry.definitionId()) != null && !SocketableDefinitions.get(entry.definitionId()).unique())
                .filter(entry -> !SocketableNames.hasAffix(entry.currentEffectName()))
                .toList();

        assertEquals(List.of(), nameless, "These pool effects shipped in a release but have no row in " + SocketableNames.AFFIXES_PATH
                + ". Common items keep the effect in their frozen name, so the name loses that word. Keep the affix row even after"
                + " the effect leaves the pool.");
    }
}
