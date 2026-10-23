package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.skills.DescriptionLine;
import exiledsector.skills.NodeDescription;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.npc.RealSkillData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        assertEquals(List.of("Low Tech Affinity: 10", "4% more hull points.", "2% more armor.", "2% more cargo capacity.",
                "2% more fuel capacity.", "2% more crew capacity."), plain(description.effects()));
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
        assertEquals(List.of("2% more flux capacity for every 5 High Tech Affinity", "2% more flux dissipation for every 10 High Tech Affinity",
                "1% more cargo capacity for every 5 High Tech Affinity", "1% more fuel capacity for every 5 High Tech Affinity",
                "1% more crew capacity for every 5 High Tech Affinity"), plain(description.details()));
    }

    private static SkillNode root(String typeId) {
        return SkillTree.getAllNodes().values().stream()
                .filter(node -> node.getType().getTier() == SkillTier.ROOT && typeId.equals(node.getType().getId())).findFirst().orElseThrow();
    }

    private static List<String> plain(List<DescriptionLine> lines) {
        return lines.stream().map(DescriptionLine::plain).toList();
    }
}
