package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.DescriptionLine;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AffinityTooltipLinesTest {

    @BeforeAll
    static void loadRealTree() throws Exception {
        RealSkillData.load();
    }

    @AfterAll
    static void clear() {
        RealSkillData.clear();
    }

    @Test
    void aRootShowsOnlyTheBonusesItsAffinityCurrentlyGrants() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root("root_low_tech"));

        NodeDescription description = AffinityTooltipLines.forRoot(root("root_low_tech").getType(), data, HullSize.CRUISER);

        assertEquals(List.of("Low Tech Affinity: 10", "4% more hull points.", "2% more fuel capacity."),
                plain(description.effects()));
    }

    @Test
    void anotherRootShowsItsOwnAffinityTotalWithoutBonusesBelowAStep() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root("root_low_tech"));

        NodeDescription description = AffinityTooltipLines.forRoot(root("root_high_tech").getType(), data, HullSize.CRUISER);

        assertEquals(List.of("High Tech Affinity: 0"), plain(description.effects()));
    }

    @Test
    void theDetailsListWhatEveryStepOfTheRootsAffinityGives() {
        NodeDescription description = AffinityTooltipLines.forRoot(root("root_high_tech").getType(), new ShipSkillData(), HullSize.CRUISER);

        assertEquals(List.of(), description.effects());
        assertEquals(List.of("2% more flux capacity for every 5 High Tech Affinity", "1% more flux dissipation for every 10 High Tech Affinity",
                "1% more crew capacity for every 5 High Tech Affinity"), plain(description.details()));
    }

    @Test
    void anAllocatedAffinityNodeShowsTheShipsTotalAndItsBonuses() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root("root_midline"));
        SkillNode affinityNode = affinityNode();
        SkillType midline = SkillTree.getType("affinity_midline");
        ShipTrees.allocate(data, affinityNode, midline, 0);

        NodeDescription description = AffinityTooltipLines.forNode(affinityNode, midline, data, HullSize.CRUISER);

        assertEquals("Midline Affinity: 15", plain(description.effects()).get(0));
        assertTrue(plain(description.effects()).contains("3% more cargo capacity."));
        assertEquals(List.of("2% more flux capacity for every 5 Midline Affinity", "2% more hull points for every 5 Midline Affinity",
                "1% more armor for every 20 Midline Affinity", "1% more flux dissipation for every 10 Midline Affinity",
                "1% more cargo capacity for every 5 Midline Affinity"), plain(description.details()));
    }

    @Test
    void anUnallocatedAffinityNodeShowsOnlyWhatEachStepGives() {
        ShipSkillData data = new ShipSkillData();
        ShipTrees.chooseStartingRoot(data, root("root_low_tech"));

        NodeDescription description = AffinityTooltipLines.forNode(affinityNode(), SkillTree.getType("affinity_low_tech"), data, HullSize.CRUISER);

        assertEquals(List.of(), description.effects());
        assertTrue(plain(description.details()).contains("1% more fuel capacity for every 5 Low Tech Affinity"));
    }

    private static SkillNode affinityNode() {
        return SkillTree.getAllNodes().values().stream().filter(node -> "small_flux_optional".equals(node.getType().getId())).findFirst().orElseThrow();
    }

    private static SkillNode root(String typeId) {
        return SkillTree.getAllNodes().values().stream()
                .filter(node -> node.getType().getTier() == SkillTier.ROOT && typeId.equals(node.getType().getId())).findFirst().orElseThrow();
    }

    private static List<String> plain(List<DescriptionLine> lines) {
        return lines.stream().map(DescriptionLine::plain).toList();
    }
}
