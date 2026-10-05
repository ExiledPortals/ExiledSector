package exiledsector.i18n;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeBonusSummary;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.skilleffect.CompatSkillEffect;
import exiledsector.skills.skilleffect.SkillEffect;
import exiledsector.skills.skilleffect.SkillEffectNames;
import lunalib.lunaSettings.LunaSettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DescriptionGoldenTest {

    static final Path GOLDEN_FILE = Path.of("src/test/resources/i18n/descriptions.en.golden");
    private static final Path ACTUAL_FILE = Path.of("target/i18n/descriptions.en.actual");
    private static final String UPDATE_PROPERTY = "exiledsector.updateGolden";
    private static final float[] SAMPLE_MAGNITUDES = {1f, 10f, 12.5f, 100f, -10f, -12.5f};
    private static final HullSize[] HULL_SIZES = {null, HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP};

    private static final Path HULL_MOD_NAMES_FILE = Path.of("src/test/resources/i18n/hullmod_names.csv");
    private static final Set<String> unnamedHullMods = new TreeSet<>();

    private static MockedStatic<LunaSettings> lunaSettingsMock;
    private static MockedStatic<Global> globalMock;
    private static boolean secondInCommandEnabled;

    @BeforeAll
    static void setUp() throws Exception {
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        SettingsAPI settings = mock(SettingsAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSettings).thenReturn(settings);
        Map<String, HullModSpecAPI> hullMods = namedHullModSpecs();
        when(settings.getHullModSpec(Mockito.anyString())).thenAnswer(invocation -> {
            String id = invocation.getArgument(0);
            if (!hullMods.containsKey(id)) {
                unnamedHullMods.add(id);
            }
            return hullMods.get(id);
        });
        ModManagerAPI modManager = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(modManager);
        when(modManager.isModEnabled(Mockito.anyString())).thenAnswer(invocation -> secondInCommandEnabled);
        RealSkillData.load();
    }

    private static Map<String, HullModSpecAPI> namedHullModSpecs() throws IOException {
        Map<String, HullModSpecAPI> specs = new HashMap<>();
        List<String> rows = Files.readAllLines(RealSkillData.projectRoot().resolve(HULL_MOD_NAMES_FILE), StandardCharsets.UTF_8);
        for (String row : rows.subList(1, rows.size())) {
            int comma = row.indexOf(',');
            HullModSpecAPI spec = mock(HullModSpecAPI.class);
            when(spec.getDisplayName()).thenReturn(row.substring(comma + 1));
            specs.put(row.substring(0, comma), spec);
        }
        return specs;
    }

    @AfterAll
    static void tearDown() {
        RealSkillData.clear();
        globalMock.close();
        lunaSettingsMock.close();
    }

    @Test
    void everyPlayerVisibleDescriptionMatchesTheRecordedEnglishTextAndHighlights() throws IOException {
        List<String> actual = generate();
        assertEquals(Set.of(), unnamedHullMods, "Add the in-game names of these hull mods to " + HULL_MOD_NAMES_FILE);
        Path actualFile = RealSkillData.projectRoot().resolve(ACTUAL_FILE);
        Files.createDirectories(actualFile.getParent());
        Files.writeString(actualFile, String.join("\n", actual) + "\n", StandardCharsets.UTF_8);
        Path golden = RealSkillData.projectRoot().resolve(GOLDEN_FILE);
        if (Boolean.getBoolean(UPDATE_PROPERTY)) {
            Files.createDirectories(golden.getParent());
            Files.writeString(golden, String.join("\n", actual) + "\n", StandardCharsets.UTF_8);
        }
        assertTrue(Files.isRegularFile(golden), "Missing " + golden + "; run with -D" + UPDATE_PROPERTY + "=true");
        List<String> expected = Files.readAllLines(golden, StandardCharsets.UTF_8);
        assertEquals(expected.size(), actual.size(), "line count");
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i), "golden line " + (i + 1));
        }
    }

    static List<String> generate() {
        List<String> lines = new ArrayList<>();
        secondInCommandEnabled = true;
        for (String name : new TreeSet<>(SkillEffectNames.all())) {
            SkillEffect effect = SkillEffect.byName(name);
            for (float magnitude : SAMPLE_MAGNITUDES) {
                String key = name + "|" + magnitude;
                addLine(lines, "effect|" + key, effect.description(magnitude), effect.lowerIsBetter());
                addLine(lines, "warning|" + key, effect.deallocationWarning(magnitude), false);
            }
        }
        secondInCommandEnabled = false;
        for (CompatSkillEffect effect : CompatSkillEffect.values()) {
            for (float magnitude : SAMPLE_MAGNITUDES) {
                addLine(lines, "effect-noSiC|" + effect.name() + "|" + magnitude, effect.description(magnitude), effect.lowerIsBetter());
            }
        }
        secondInCommandEnabled = true;
        for (SkillType type : sortedTypes()) {
            for (HullSize hullSize : HULL_SIZES) {
                NodeDescription description = SkillNode.describeType(type, hullSize);
                for (int i = 0; i < description.effects().size(); i++) {
                    DescriptionLine line = description.effects().get(i);
                    addLine(lines, "type|" + type.getId() + "|" + hullSize + "|" + i, line.text(), line.lowerIsBetter());
                }
                for (int i = 0; i < description.details().size(); i++) {
                    DescriptionLine line = description.details().get(i);
                    addLine(lines, "type-detail|" + type.getId() + "|" + hullSize + "|" + i, line.text(), line.lowerIsBetter());
                }
            }
        }
        for (int option = 0; option < 3; option++) {
            ShipSkillData everything = everyNodeAllocated(option);
            for (HullSize hullSize : HULL_SIZES) {
                if (hullSize == null) {
                    continue;
                }
                List<DescriptionLine> bonuses = SkillTreeBonusSummary.of(everything, hullSize).bonuses();
                for (int i = 0; i < bonuses.size(); i++) {
                    DescriptionLine line = bonuses.get(i);
                    addLine(lines, "summary|option" + option + "|" + hullSize + "|" + i, line.text(), line.lowerIsBetter());
                }
            }
        }
        return lines;
    }

    private static List<SkillType> sortedTypes() {
        List<SkillType> types = new ArrayList<>(SkillTree.getAllTypes().values());
        types.sort((a, b) -> a.getId().compareTo(b.getId()));
        return types;
    }

    private static ShipSkillData everyNodeAllocated(int option) {
        ShipSkillData data = new ShipSkillData();
        List<SkillNode> nodes = new ArrayList<>(SkillTree.getAllNodes().values());
        nodes.sort((a, b) -> a.getId().compareTo(b.getId()));
        for (SkillNode node : nodes) {
            if (node.getType().getTier() == SkillTier.ROOT && data.getAllocatedNodeIds().stream()
                    .anyMatch(id -> SkillTree.get(id).getType().getTier() == SkillTier.ROOT)) {
                continue;
            }
            List<String> options = node.getType().getOptionalOptionIds();
            if (options.isEmpty()) {
                data.allocate(node, 0);
            } else {
                data.selectOption(node, SkillTree.getType(options.get(Math.min(option, options.size() - 1))), 0);
            }
        }
        return data;
    }

    private static void addLine(List<String> lines, String key, StyledText text, boolean lowerIsBetter) {
        if (text == null) {
            return;
        }
        if (text.plain().indexOf('<') >= 0) {
            throw new IllegalStateException("Description text contains '<': " + text.plain());
        }
        lines.add(key + "\t" + (lowerIsBetter ? "1" : "0") + "\t" + escape(text.toMarkup()));
    }

    private static String escape(String text) {
        return text.replace("\\", "\\\\").replace("\n", "\\n").replace("\t", "\\t");
    }
}
