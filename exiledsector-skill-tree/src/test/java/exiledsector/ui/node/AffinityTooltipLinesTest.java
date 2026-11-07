package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.effects.SkillTreeEffectSource;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.EffectContribution;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.SkillTypeEffect;
import exiledsector.skills.SocketedItemScaling;
import exiledsector.skills.npc.RealSkillData;
import exiledsector.skills.skilleffect.fighter.FighterSkillEffect;
import exiledsector.skills.skilleffect.onhit.OnHitSkillEffect;
import exiledsector.skills.skilleffect.special.SpecialSkillEffect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffinityTooltipLinesTest {

    private static final HullSize HULL_SIZE = HullSize.CRUISER;

    @BeforeAll
    static void loadRealTree() throws Exception {
        RealSkillData.load();
    }

    @AfterAll
    static void clear() {
        RealSkillData.clear();
    }

    @Test
    void aRootShowsTheShipsTotalAndTheBonusesItsAffinityCurrentlyGrants() {
        NodeDescription description = describe(root("root_low_tech").getType(), lowTechShip());

        assertEquals(List.of("""
                Current Low Tech Affinity: 10
                Your Low Tech Affinity currently grants:
                • 4% more hull points.
                • 2% more fuel capacity."""), plain(description.effects()));
    }

    @Test
    void anotherRootShowsItsOwnAffinityTotalWithoutBonusesBelowAStep() {
        NodeDescription description = describe(root("root_high_tech").getType(), lowTechShip());

        assertEquals(List.of("Current High Tech Affinity: 0"), plain(description.effects()));
    }

    @Test
    void theSeparatedDetailsListWhatEveryStepOfTheAffinityGives() {
        NodeDescription description = describe(root("root_high_tech").getType(), new ShipSkillData());

        assertEquals(List.of("""
                High Tech Affinity grants:
                • 2% more flux capacity for every 5 High Tech Affinity.
                • 1% more flux dissipation for every 10 High Tech Affinity.
                • 1% more crew capacity for every 5 High Tech Affinity."""), plain(description.details()));
    }

    @Test
    void anAffinityNodeShowsTheShipsTotalWhetherOrNotItIsAllocated() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root("root_midline"));
        SkillType midline = SkillTree.getType("midline_affinity");

        assertEquals("Current Midline Affinity: 10", firstLine(describe(midline, data)));
        ShipTrees.allocate(data, affinityNode(), midline, 0);
        NodeDescription allocated = describe(midline, data);

        assertEquals("Current Midline Affinity: 15", firstLine(allocated));
        assertEquals(List.of("""
                Midline Affinity grants:
                • 2% more flux capacity for every 5 Midline Affinity.
                • 1% more hull points for every 5 Midline Affinity.
                • 1% more armor for every 20 Midline Affinity.
                • 1% more flux dissipation for every 20 Midline Affinity.
                • 1% more cargo capacity for every 5 Midline Affinity."""), plain(allocated.details()));
    }

    @Test
    void hullItemsThatReadAnAffinityJoinItsCurrentAndPerStepLines() {
        List<EffectContribution> contributions = new ArrayList<>(contributions(lowTechShip()));
        contributions.add(hullItem(new SkillTypeEffect(SpecialSkillEffect.LOW_TECH_AFFINITY_FLAT, 10f),
                new SkillTypeEffect(SpecialSkillEffect.ARMOR_PERCENT_PER_LOW_TECH_AFFINITY, 1f),
                new SkillTypeEffect(SpecialSkillEffect.WEAPON_RANGE_PERCENT_PER_AFFINITY, 1f),
                new SkillTypeEffect(SpecialSkillEffect.ENERGY_WEAPON_DAMAGE_PERCENT_PER_HIGH_TECH_AFFINITY, 1f)));

        NodeDescription description = AffinityTooltipLines.forType(root("root_low_tech").getType(), contributions, HULL_SIZE);

        assertEquals(List.of("""
                Current Low Tech Affinity: 20
                Your Low Tech Affinity currently grants:
                • 8% more hull points.
                • 1% more armor.
                • 1% more top speed.
                • 4% more fuel capacity.
                • 2% increased armor.""", """
                Your total affinity currently grants:
                • 2% increased weapon range."""), plain(description.effects()));
        List<String> perStep = plain(description.details()).get(0).lines().toList();
        assertEquals(List.of("• 1% increased armor for every 10 Low Tech Affinity.", "• 1% increased weapon range for every 10 affinity."),
                perStep.subList(perStep.size() - 2, perStep.size()));
    }

    @Test
    void anEffectReadingTheHighestAffinityShowsOnlyOnTheRootOfTheHighestAffinity() {
        List<EffectContribution> contributions = new ArrayList<>(contributions(lowTechShip()));
        contributions.add(hullItem(new SkillTypeEffect(SpecialSkillEffect.MISSILE_HIT_RIFT_SERIES_CHANCE_PERCENT_PER_HIGHEST_AFFINITY, 1f)));
        String currentLine = plain(List.of(new DescriptionLine(OnHitSkillEffect.MISSILE_HIT_RIFT_SERIES_CHANCE_PERCENT.description(2f, HULL_SIZE),
                false))).get(0);
        String perStepLine = plain(List.of(new DescriptionLine(
                SpecialSkillEffect.MISSILE_HIT_RIFT_SERIES_CHANCE_PERCENT_PER_HIGHEST_AFFINITY.description(1f, HULL_SIZE), false))).get(0);

        NodeDescription lowTech = AffinityTooltipLines.forType(root("root_low_tech").getType(), contributions, HULL_SIZE);
        NodeDescription highTech = AffinityTooltipLines.forType(root("root_high_tech").getType(), contributions, HULL_SIZE);

        assertTrue(plain(lowTech.effects()).get(0).contains("Your Low Tech Affinity currently grants:"));
        assertTrue(joined(lowTech.effects()).contains(currentLine));
        assertTrue(plain(lowTech.details()).get(0).contains(perStepLine));
        assertFalse(joined(highTech.effects()).contains(currentLine));
        assertFalse(joined(highTech.details()).contains(perStepLine));
    }

    @Test
    void otherAffinityEffectsOnTheShipFollowThePerStepLines() {
        List<EffectContribution> contributions = new ArrayList<>(contributions(lowTechShip()));
        contributions.add(hullItem(new SkillTypeEffect(SpecialSkillEffect.HIGHEST_AFFINITY_FLAT_PER_S_MOD, 6f),
                new SkillTypeEffect(SpecialSkillEffect.LOWEST_AFFINITY_AS_HIGHEST, 1f),
                new SkillTypeEffect(FighterSkillEffect.STRIKECRAFT_GAIN_AFFINITY_BONUSES, 1f)));

        NodeDescription description = AffinityTooltipLines.forType(root("root_low_tech").getType(), contributions, HULL_SIZE);

        assertEquals("Current Low Tech Affinity: 16", firstLine(description));
        assertEquals("""
                Other affinity effects on this ship:
                • +6 to your highest affinity from s-mods.
                • Your lowest affinity counts as equal to your highest.
                • Strikecraft launched from this ship gain all of its affinity bonuses except logistics ones.""",
                plain(description.details()).get(1));
    }

    private static ShipSkillData lowTechShip() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root("root_low_tech"));
        return data;
    }

    private static NodeDescription describe(SkillType type, ShipSkillData data) {
        return AffinityTooltipLines.forType(type, contributions(data), HULL_SIZE);
    }

    private static List<EffectContribution> contributions(ShipSkillData data) {
        return SocketedItemScaling.withScaledItems(SkillTreeEffectSource.contributions(data, HULL_SIZE));
    }

    private static EffectContribution hullItem(SkillTypeEffect... effects) {
        return EffectContribution.scaledPermanent("hull item", "item_", List.of(effects));
    }

    private static String firstLine(NodeDescription description) {
        return plain(description.effects()).get(0).lines().findFirst().orElseThrow();
    }

    private static SkillNode affinityNode() {
        return SkillTree.getAllNodes().values().stream().filter(node -> "affinity".equals(node.getType().getId())).findFirst().orElseThrow();
    }

    private static SkillNode root(String typeId) {
        return SkillTree.getAllNodes().values().stream()
                .filter(node -> node.getType().getTier() == SkillTier.ROOT && typeId.equals(node.getType().getId())).findFirst().orElseThrow();
    }

    private static List<String> plain(List<DescriptionLine> lines) {
        return lines.stream().map(DescriptionLine::plain).toList();
    }

    private static String joined(List<DescriptionLine> lines) {
        return String.join("\n", plain(lines));
    }
}
