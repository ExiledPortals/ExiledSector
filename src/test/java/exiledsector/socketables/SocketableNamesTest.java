package exiledsector.socketables;

import exiledsector.skills.npc.RealSkillData;
import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketableNamesTest {

    private static final Path NAMES = Path.of(SocketableNames.NAMES_PATH);
    private static final Path AFFIXES = Path.of(SocketableNames.AFFIXES_PATH);
    private static final RolledEffect DISSIPATION = new RolledEffect("FLUX_DISSIPATION_MULT", 5f);
    private static final RolledEffect SHIELDING = new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f);
    private static final RolledEffect HULL = new RolledEffect("HULL_MULT", 5f);
    private static final RolledEffect ARMOR = new RolledEffect("ARMOR_PERCENT", 7f);

    @BeforeEach
    void setUp() throws Exception {
        Path root = RealSkillData.projectRoot();
        SocketableNames.registerWords(new JSONObject(Files.readString(root.resolve(NAMES), StandardCharsets.UTF_8)));
        SocketableNames.registerAffixes(CDL.toJSONArray(Files.readString(root.resolve(AFFIXES), StandardCharsets.UTF_8)
                .replace("\r\n", "\n")));
        SocketableDefinitions.register(new JSONArray()
                .put(SocketableFixtures.row("military", "subroutine", SocketableFixtures.MILITARY_PREFIXES,
                        SocketableFixtures.MILITARY_SUFFIXES).put("grade", "military"))
                .put(SocketableFixtures.row("industrial", "subroutine", "HULL_MULT:4:6").put("grade", "industrial")
                        .put("name", "Industrial-grade Domain Subroutine"))
                .put(SocketableFixtures.row("consumer", "subroutine", "HULL_MULT:4:6").put("grade", "consumer")
                        .put("name", "Consumer-grade Domain Subroutine"))
                .put(SocketableFixtures.row("relic", "ai_core", "HULL_MULT:4:6; ARMOR_PERCENT:6:9; FLUX_CAPACITY_MULT:4:6")
                        .put("name", "Omega Shard").put("unique", "TRUE")));
    }

    @AfterEach
    void tearDown() {
        SocketableNames.clear();
        SocketableDefinitions.clear();
    }

    private static SocketableName name(String definitionId, long seed, List<RolledEffect> effects) {
        SocketableDefinition definition = SocketableDefinitions.get(definitionId);
        return SocketableNames.nameFor(definition, definition.kind(), seed, effects);
    }

    @Test
    void twoEffectsMakeAMagicItemNamedAfterThemWithItsTypeUnderneath() {
        SocketableName name = name("military", 1L, List.of(DISSIPATION, SHIELDING));

        assertEquals(SocketableRarity.MAGIC, name.rarity());
        assertEquals("Vent-efficient military-grade subroutine of Shielding", name.title());
        assertEquals("Military-grade Domain Subroutine", name.baseName());
    }

    @Test
    void theMagicNameFollowsEachEffectsRoleNotItsOrder() {
        assertEquals("Vent-efficient military-grade subroutine of Shielding", name("military", 1L, List.of(SHIELDING, DISSIPATION)).title());
    }

    @Test
    void aMagicItemMissingAnAffixKeepsTheOtherOne() throws Exception {
        SocketableNames.registerAffixes(new JSONArray()
                .put(new JSONObject().put("effect", "FLUX_DISSIPATION_MULT").put("prefix", "Dissipation").put("suffix", ""))
                .put(new JSONObject().put("effect", "HULL_MULT").put("prefix", "").put("suffix", "Fortitude")));

        assertEquals("Dissipation military-grade subroutine", name("military", 1L, List.of(DISSIPATION, ARMOR)).title());
        assertEquals("Military-grade subroutine of Fortitude",
                name("military", 1L, List.of(new RolledEffect("REMOVED_EFFECT", 1f), HULL)).title());
    }

    @Test
    void threeOrFourEffectsMakeARareCodenameOverTheGrade() {
        Set<String> names = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            SocketableName name = name("military", seed, List.of(DISSIPATION, SHIELDING, HULL));
            assertEquals(SocketableRarity.RARE, name.rarity());
            assertEquals("Military-grade Domain Subroutine", name.baseName());
            assertTrue(name.title().matches("[A-Z][a-z]+ [A-Z][a-z]+"), name.title());
            names.add(name.title());
        }
        assertEquals(name("military", 7L, List.of(DISSIPATION, SHIELDING, HULL)),
                name("military", 7L, List.of(DISSIPATION, SHIELDING, HULL, ARMOR)));
        assertTrue(names.size() > 50, "names should vary: " + names.size());
    }

    @Test
    void industrialRaresAreTwoWordNamesWithNoRevisionNumber() {
        for (long seed = 0; seed < 200; seed++) {
            String title = name("industrial", seed, List.of(HULL, ARMOR, DISSIPATION)).title();
            assertTrue(title.matches("[A-Z][a-z]+ [A-Z][a-z]+"), title);
        }
    }

    @Test
    void consumerRaresAreProductNamesThatSometimesCompoundOrAddAModel() {
        boolean compound = false;
        boolean model = false;
        for (long seed = 0; seed < 400; seed++) {
            String title = name("consumer", seed, List.of(HULL, ARMOR, DISSIPATION)).title();
            String[] words = title.split(" ");
            assertTrue(words.length == 2 || words.length == 3, title);
            assertNotEquals(words[words.length - 1], words[words.length - 2], title);
            compound |= words[0].matches("[A-Z][a-z]+[A-Z][a-z]+");
            model |= words.length == 3;
        }
        assertTrue(compound && model, "expected both compound brands and model suffixes");
    }

    @Test
    void uniquesKeepTheirHandWrittenNameAndRollEveryEffectInOrder() {
        SocketableDefinition relic = SocketableDefinitions.get("relic");
        List<RolledEffect> effects = SocketableRoller.roll(relic, 3L);

        assertEquals(List.of("HULL_MULT", "ARMOR_PERCENT", "FLUX_CAPACITY_MULT"), effects.stream().map(RolledEffect::effectName).toList());
        SocketableName name = SocketableNames.nameFor(relic, relic.kind(), 3L, effects);
        assertEquals(new SocketableName("Omega Shard", null, SocketableRarity.UNIQUE), name);
    }

    @Test
    void everyShippedPoolEffectHasBothAffixesAndEveryShippedGradeHasWords() throws Exception {
        String csv = Files.readString(RealSkillData.projectRoot().resolve(SocketableDefinitions.DATA_PATH), StandardCharsets.UTF_8);
        SocketableDefinitions.register(CDL.toJSONArray(csv.replace("\r\n", "\n")));
        JSONObject words = new JSONObject(Files.readString(RealSkillData.projectRoot().resolve(NAMES), StandardCharsets.UTF_8));
        for (SocketableDefinition definition : SocketableDefinitions.all()) {
            if (definition.unique()) {
                continue;
            }
            assertTrue(words.has(definition.grade()), definition.grade() + " has no rare name words");
            for (SocketableDefinition.PoolEntry entry : definition.pool()) {
                assertTrue(SocketableNames.hasAffix(entry.effectName()), entry.effectName() + " has no affix row");
                assertFalse(name(definition.id(), 1L, List.of(new RolledEffect(entry.effectName(), 1f),
                        new RolledEffect(entry.effectName(), 1f))).title().contains("  "));
            }
        }
    }
}
