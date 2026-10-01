package exiledsector.skills;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.SkillTreeBonusSummary.Summary;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.skilleffect.FluxSkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTreeBonusSummaryTest {

    private ShipSkillData data;

    @BeforeEach
    void setUp() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillNode root = register("root_1", new SkillType.Builder("root", "Low Tech", "a.png", SkillTier.ROOT)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 10f))).build());
        data = new ShipSkillData();
        data.chooseStartingRoot(root);
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
    }

    private static SkillNode register(String id, SkillType type) {
        SkillTree.registerType(type);
        SkillNode node = new SkillNode(id, type, List.of(), 0f, 0f);
        SkillTree.register(node);
        return node;
    }

    private static SkillType small(String id, SkillTypeEffect... effects) {
        return new SkillType.Builder(id, id, "a.png", SkillTier.SMALL).effects(List.of(effects)).build();
    }

    private void allocate(String nodeId, SkillType type) {
        data.allocate(register(nodeId, type), 0);
    }

    private static List<String> texts(Summary summary) {
        return summary.bonuses().stream().map(DescriptionLine::plain).toList();
    }

    @Test
    void percentBonusesFromSeveralNodesAndTheRootAddUp() {
        SkillType hull = small("hull", new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 5f));
        allocate("hull_1", hull);
        allocate("hull_2", hull);

        Summary summary = SkillTreeBonusSummary.of(data, HullSize.CRUISER);

        assertEquals(List.of(DefenseSkillEffect.HULL_PERCENT.description(20f).plain()), texts(summary));
    }

    @Test
    void multiplicativeBonusesCompound() {
        SkillType flux = small("flux", new SkillTypeEffect(FluxSkillEffect.FLUX_CAPACITY_MULT, 10f));
        allocate("flux_1", flux);
        allocate("flux_2", flux);

        Summary summary = SkillTreeBonusSummary.of(data, HullSize.CRUISER);

        assertTrue(texts(summary).contains(FluxSkillEffect.FLUX_CAPACITY_MULT.description(21f).plain()), texts(summary).toString());
    }

    @Test
    void hullSizeEffectsUseTheShipsHullSize() {
        allocate("armor_1", new SkillType.Builder("armor", "Armor", "a.png", SkillTier.SMALL)
                .hullSizeEffects(List.of(new HullSizeSkillEffect(DefenseSkillEffect.ARMOR_FLAT, 30f, 50f, 100f, 125f))).build());

        Summary summary = SkillTreeBonusSummary.of(data, HullSize.DESTROYER);

        assertTrue(texts(summary).contains(DefenseSkillEffect.ARMOR_FLAT.description(50f).plain()), texts(summary).toString());
    }

    @Test
    void temporaryEffectsAreListedSeparatelyWithTheirDuration() {
        allocate("temp_1", new SkillType.Builder("temp", "Temp", "a.png", SkillTier.SMALL)
                .effects(List.of(new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 30f)))
                .temporaryAfterDeploymentSeconds(60f).build());

        Summary summary = SkillTreeBonusSummary.of(data, HullSize.CRUISER);

        assertEquals(List.of(DefenseSkillEffect.HULL_PERCENT.description(10f).plain(),
                "For the first 60 seconds after deployment: " + DefenseSkillEffect.HULL_PERCENT.description(30f).plain()), texts(summary));
    }

    @Test
    void theRootIsReportedSeparatelyFromTheNodeCountAndNotablesAreListed() {
        allocate("hull_1", small("hull", new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 5f)));
        SkillType notable = new SkillType.Builder("heavyarmor", "Heavy Armor", "a.png", SkillTier.NOTABLE).build();
        SkillType keystone = new SkillType.Builder("rangefinder", "Ballistic Rangefinder", "a.png", SkillTier.KEYSTONE).build();
        allocate("heavyarmor_1", notable);
        allocate("rangefinder_1", keystone);

        Summary summary = SkillTreeBonusSummary.of(data, HullSize.CRUISER);

        assertEquals("Low Tech", summary.root().getDisplayName());
        assertEquals(3, summary.nodeCount());
        assertEquals(List.of("Heavy Armor", "Ballistic Rangefinder"),
                summary.notables().stream().map(SkillType::getDisplayName).toList());
    }

    @Test
    void anOptionalNodeContributesItsChosenOption() {
        SkillType hullOption = small("hull_option", new SkillTypeEffect(DefenseSkillEffect.HULL_PERCENT, 2f));
        SkillTree.registerType(hullOption);
        SkillNode optional = register("optional_1", new SkillType.Builder("optional", "Optional", "a.png", SkillTier.SMALL)
                .optionalOptionIds(List.of("hull_option")).build());
        data.selectOption(optional, hullOption, 0);

        Summary summary = SkillTreeBonusSummary.of(data, HullSize.CRUISER);

        assertEquals(List.of(DefenseSkillEffect.HULL_PERCENT.description(12f).plain()), texts(summary));
    }

    @Test
    void npcBuildsLeaveOutEffectsThatNeverApplyToNpcShips() {
        allocate("bulkheads_1", small("reinforcedhull",
                new SkillTypeEffect(DefenseSkillEffect.SHIP_RECOVERY_CHANCE_BONUS, 1000f)));
        data.markNpcBuild();

        Summary summary = SkillTreeBonusSummary.of(data, HullSize.CRUISER);

        assertEquals(List.of(DefenseSkillEffect.HULL_PERCENT.description(10f).plain()), texts(summary));
    }

    @Test
    void theLevelIsReported() {
        data.incrementLevel();
        data.incrementLevel();

        assertEquals(2, SkillTreeBonusSummary.of(data, HullSize.FRIGATE).level());
    }
}
