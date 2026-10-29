package exiledsector.i18n;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.ProjectFiles;
import exiledlib.i18n.HeadlessGameText;
import exiledlib.i18n.StyledText;
import exiledlib.skills.DescriptionLine;
import exiledlib.skills.skilleffect.SkillEffect;
import exiledlib.skills.skilleffect.SkillEffectNames;
import exiledlib.skills.skilleffect.fighter.FighterSkillEffect;
import exiledlib.skills.skilleffect.shipsystem.ShipSystemSkillEffect;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillTreeBonusSummary;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptionGoldenTest {

    static final Path GOLDEN_FILE = Path.of("exiledsector-skill-tree/src/test/resources/i18n/descriptions.en.golden");
    private static final Path ACTUAL_FILE = Path.of("target/i18n/descriptions.en.actual");
    private static final String UPDATE_PROPERTY = "exiledsector.updateGolden";
    private static final float[] SAMPLE_MAGNITUDES = {1f, 10f, 12.5f, 100f, -10f, -12.5f};
    private static final HullSize[] HULL_SIZES = {null, HullSize.FRIGATE, HullSize.DESTROYER, HullSize.CRUISER, HullSize.CAPITAL_SHIP};

    private static HeadlessGameText headlessGameText;
    private static boolean secondInCommandEnabled;

    @BeforeAll
    static void setUp() throws Exception {
        headlessGameText = HeadlessGameText.install(() -> secondInCommandEnabled);
        RealSkillData.load();
    }

    @AfterAll
    static void tearDown() {
        RealSkillData.clear();
        headlessGameText.close();
    }

    @Test
    void everyPlayerVisibleDescriptionMatchesTheRecordedEnglishTextAndHighlights() throws IOException {
        List<String> actual = generate();
        assertEquals(Set.of(), headlessGameText.unnamedHullMods(), "Add the in-game names of these hull mods to " + HeadlessGameText.HULL_MOD_NAMES_FILE);
        Path actualFile = ProjectFiles.projectRoot().resolve(ACTUAL_FILE);
        Files.createDirectories(actualFile.getParent());
        Files.writeString(actualFile, String.join("\n", actual) + "\n", StandardCharsets.UTF_8);
        Path golden = ProjectFiles.projectRoot().resolve(GOLDEN_FILE);
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
                addLine(lines, "effect|" + key, effect.description(magnitude), effect.invertsDescriptionColours());
                addLine(lines, "warning|" + key, effect.deallocationWarning(magnitude), false);
            }
        }
        secondInCommandEnabled = false;
        for (SkillEffect effect : List.of(ShipSystemSkillEffect.AUGMENTED_SYSTEM_REGEN_PERCENT, FighterSkillEffect.CONVERTED_HANGAR_REFIT_TIME_MULT,
                FighterSkillEffect.CONVERTED_HANGAR_REPLACEMENT_RATE_MULT, FighterSkillEffect.CONVERTED_HANGAR_RELAUNCH_TIME_FLAT,
                FighterSkillEffect.CONVERTED_HANGAR_MIN_CREW_FLAT)) {
            for (float magnitude : SAMPLE_MAGNITUDES) {
                addLine(lines, "effect-noSiC|" + effect.name() + "|" + magnitude, effect.description(magnitude), effect.invertsDescriptionColours());
            }
        }
        secondInCommandEnabled = true;
        for (SkillType type : sortedTypes()) {
            if (type.getFlavourText() != null) {
                addLine(lines, "flavour|" + type.getId(), StyledText.of(type.getFlavourText()), false);
            }
            if (type.getAutomatedFlavourText() != null) {
                addLine(lines, "flavourAutomated|" + type.getId(), StyledText.of(type.getAutomatedFlavourText()), false);
            }
            addLine(lines, "itemCost|" + type.getId(), SkillNode.describeItemCost(type.getItemCost(), null), false);
            for (HullSize hullSize : HULL_SIZES) {
                NodeDescription description = SkillNode.describeMechanics(type, hullSize);
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
                ShipTrees.allocate(data, node, 0);
            } else {
                ShipTrees.selectOption(data, node, SkillTree.getType(options.get(Math.min(option, options.size() - 1))), 0);
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
