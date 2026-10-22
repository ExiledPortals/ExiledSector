package exiledsector.items;

import exiledlib.ProjectFiles;
import exiledlib.items.ItemDefinition;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemModifiers;
import exiledlib.items.ItemName;
import exiledlib.items.ItemNames;
import exiledlib.items.ItemRarity;
import exiledlib.items.ItemRoller;
import exiledlib.items.ItemType;
import exiledlib.items.PoolEntry;
import exiledlib.items.RolledEffect;
import exiledlib.items.RolledModifier;
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

class ItemNamesTest {

    private static final Path NAMES = Path.of(ItemNames.namesPath());
    private static final RolledEffect DISSIPATION = new RolledEffect("FLUX_DISSIPATION_MULT", 5f);
    private static final RolledEffect SHIELDING = new RolledEffect("SHIELD_DAMAGE_TAKEN_MULT", -12f);
    private static final RolledEffect HULL = new RolledEffect("HULL_MULT", 5f);
    private static final RolledEffect ARMOR = new RolledEffect("ARMOR_PERCENT", 7f);

    @BeforeEach
    void setUp() throws Exception {
        Path root = ProjectFiles.projectRoot();
        ItemNames.registerWords(new JSONObject(Files.readString(root.resolve(NAMES), StandardCharsets.UTF_8))
                .put("military_bridge", words("codename", List.of("Askonia", "Corvus", "Galatia", "Hybrasil", "Magec", "Duzahk", "Thule", "Aztlan",
                                "Samarra", "Zagan", "Valhalla", "Mayasura", "Eos", "Canaan"),
                        List.of("Battlegroup", "Doctrine", "Protocol", "Directive", "Picket", "Cordon", "Vanguard", "Sentinel", "Mandate", "Interdict",
                                "Tribunal", "Overwatch"), List.of()))
                .put("industrial_bridge", words("codename", List.of("Ilmari", "Hephaestus", "Basalt", "Tungsten", "Anvil", "Gantry"),
                        List.of("Governor", "Regulator", "Manifold", "Coupling", "Loom", "Spindle"), List.of()))
                .put("consumer_bridge", words("product", List.of("Sunny", "Happy", "Comfort", "Easy", "Safe", "Family"),
                        List.of("Pal", "Helper", "Buddy", "Assist", "Companion", "Navigator"), List.of("3000", "Plus", "Pro", "Max"))));
        ItemFixtures.registerModifierNames("FLUX_DISSIPATION_MULT,prefix,FLUX_DISSIPATION_MULT:4:6,Vent-efficient",
                "SHIELD_DAMAGE_TAKEN_MULT,suffix,SHIELD_DAMAGE_TAKEN_MULT:-15:-10,Shielding", "HULL_MULT,suffix,HULL_MULT:4:6,Fortitude");
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row("military_bridge", "bridge", ItemFixtures.MILITARY_PREFIXES,
                        ItemFixtures.MILITARY_SUFFIXES))
                .put(ItemFixtures.row("industrial_bridge", "bridge", "HULL_MULT:4:6")
                        .put("name", "Industrial-grade Bridge"))
                .put(ItemFixtures.row("consumer_bridge", "bridge", "HULL_MULT:4:6")
                        .put("name", "Consumer-grade Bridge"))
                .put(ItemFixtures.row("relic", "reactor", "HULL_MULT:4:6; ARMOR_PERCENT:6:9; FLUX_CAPACITY_MULT:4:6")
                        .put("name", "Omega Shard").put("unique", "TRUE")));
    }

    private static JSONObject words(String style, List<String> firstWords, List<String> secondWords, List<String> models) throws Exception {
        JSONObject wordsJson = new JSONObject().put("style", style).put("first", new JSONArray(firstWords)).put("second", new JSONArray(secondWords));
        return models.isEmpty() ? wordsJson : wordsJson.put("models", new JSONArray(models));
    }

    private static JSONArray csvRows(String dataPath) throws Exception {
        return CDL.toJSONArray(Files.readString(ProjectFiles.projectRoot().resolve(dataPath), StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }

    @AfterEach
    void tearDown() {
        ItemNames.clear();
        ItemModifiers.clear();
        ItemDefinitions.clear();
    }

    private static ItemName name(String definitionId, long seed, List<RolledEffect> effects) {
        ItemDefinition definition = ItemDefinitions.get(definitionId);
        return ItemNames.nameFor(definition, definition.kind(), seed, RolledModifier.fromLegacyEffects(definition, effects));
    }

    @Test
    void twoEffectsMakeACommonItemNamedAfterThemWithItsTypeUnderneath() {
        ItemName name = name("military_bridge", 1L, List.of(DISSIPATION, SHIELDING));

        assertEquals(ItemRarity.COMMON, name.rarity());
        assertEquals("Vent-efficient officer of Shielding", name.title());
        assertEquals("Military-grade Bridge", name.baseName());
    }

    @Test
    void theCommonNameFollowsEachEffectsRoleNotItsOrder() {
        assertEquals("Vent-efficient officer of Shielding", name("military_bridge", 1L, List.of(SHIELDING, DISSIPATION)).title());
    }

    @Test
    void aCommonItemMissingAnAffixKeepsTheOtherOne() throws Exception {
        ItemFixtures.registerModifierNames("FLUX_DISSIPATION_MULT,prefix,FLUX_DISSIPATION_MULT:4:6,Dissipation",
                "HULL_MULT,suffix,HULL_MULT:4:6,Fortitude");

        assertEquals("Dissipation officer", name("military_bridge", 1L, List.of(DISSIPATION, ARMOR)).title());
        assertEquals("Officer of Fortitude",
                name("military_bridge", 1L, List.of(new RolledEffect("REMOVED_EFFECT", 1f), HULL)).title());
    }

    @Test
    void threeOrFourEffectsMakeARareCodenameOverTheBaseName() {
        Set<String> names = new HashSet<>();
        for (long seed = 0; seed < 200; seed++) {
            ItemName name = name("military_bridge", seed, List.of(DISSIPATION, SHIELDING, HULL));
            assertEquals(ItemRarity.RARE, name.rarity());
            assertEquals("Military-grade Bridge", name.baseName());
            assertTrue(name.title().matches("[A-Z][a-z]+ [A-Z][a-z]+"), name.title());
            names.add(name.title());
        }
        assertEquals(name("military_bridge", 7L, List.of(DISSIPATION, SHIELDING, HULL)),
                name("military_bridge", 7L, List.of(DISSIPATION, SHIELDING, HULL, ARMOR)));
        assertTrue(names.size() > 50, "names should vary: " + names.size());
    }

    @Test
    void industrialRaresAreTwoWordNamesWithNoRevisionNumber() {
        for (long seed = 0; seed < 200; seed++) {
            String title = name("industrial_bridge", seed, List.of(HULL, ARMOR, DISSIPATION)).title();
            assertTrue(title.matches("[A-Z][a-z]+ [A-Z][a-z]+"), title);
        }
    }

    @Test
    void consumerRaresAreProductNamesThatSometimesCompoundOrAddAModel() {
        boolean compound = false;
        boolean model = false;
        for (long seed = 0; seed < 400; seed++) {
            String title = name("consumer_bridge", seed, List.of(HULL, ARMOR, DISSIPATION)).title();
            String[] words = title.split(" ");
            assertTrue(words.length == 2 || words.length == 3, title);
            assertNotEquals(words[words.length - 1], words[words.length - 2], title);
            compound |= words[0].matches("[A-Z][a-z]+[A-Z][a-z]+");
            model |= words.length == 3;
        }
        assertTrue(compound && model, "expected both compound brands and model suffixes");
    }

    @Test
    void aProductWordListOfOneRepeatedWordStillNamesTheItem() throws Exception {
        ItemNames.registerWords(new JSONObject()
                .put("consumer_bridge", new JSONObject().put("style", "product")
                        .put("first", new JSONArray().put("Sunny").put("Sunny").put(" Sunny "))
                        .put("second", new JSONArray().put("Pal"))));

        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(java.time.Duration.ofSeconds(5), () -> {
            for (long seed = 0; seed < 200; seed++) {
                assertEquals("Sunny Pal", name("consumer_bridge", seed, List.of(HULL, ARMOR, DISSIPATION)).title());
            }
        });
    }

    @Test
    void uniquesKeepTheirHandWrittenNameAndRollEveryEffectInOrder() {
        ItemDefinition relic = ItemDefinitions.get("relic");
        List<RolledModifier> modifiers = ItemRoller.roll(relic, 3L);

        assertEquals(List.of("HULL_MULT", "ARMOR_PERCENT", "FLUX_CAPACITY_MULT"), RolledModifier.lines(modifiers).stream().map(RolledEffect::effectName).toList());
        ItemName name = ItemNames.nameFor(relic, relic.kind(), 3L, modifiers);
        assertEquals(new ItemName("Omega Shard", null, ItemRarity.UNIQUE), name);
    }

    @Test
    void everyShippedPoolModifierHasANameAndEveryCommonHullItemHasRareNameWords() throws Exception {
        ItemModifiers.register(csvRows(ItemModifiers.dataPath()));
        ItemDefinitions.register(csvRows(SubroutineDefinitions.dataPath()), csvRows(HullItem.definitionsPath()));
        JSONObject words = new JSONObject(Files.readString(ProjectFiles.projectRoot().resolve(NAMES), StandardCharsets.UTF_8));
        for (ItemDefinition definition : ItemDefinitions.all()) {
            if (definition.unique() || definition.type() == ItemType.SUBROUTINE) {
                continue;
            }
            assertTrue(words.has(definition.id()), definition.id() + " has no rare name words");
            for (PoolEntry entry : definition.pool()) {
                assertTrue(ItemNames.hasName(definition, entry.modifier().id(), entry.slot()), entry.modifier().id() + " has no name");
                assertFalse(name(definition.id(), 1L, List.of(new RolledEffect(entry.effectName(), 1f),
                        new RolledEffect(entry.effectName(), 1f))).title().contains("  "));
            }
        }
    }
}
