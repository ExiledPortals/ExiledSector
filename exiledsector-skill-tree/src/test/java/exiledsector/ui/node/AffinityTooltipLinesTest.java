package exiledsector.ui.node;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
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
    void aRootShowsEveryAffinityTotalAndTheBonusesOnlyOfAffinitiesTheShipHas() {
        SkillNode lowTechRoot = SkillTree.getAllNodes().values().stream()
                .filter(node -> node.getType().getTier() == SkillTier.ROOT && "root_low_tech".equals(node.getType().getId())).findFirst().orElseThrow();
        ShipSkillData data = new ShipSkillData();
        assertTrue(ShipTrees.chooseStartingRoot(data, lowTechRoot));

        List<String> lines = AffinityTooltipLines.forShip(data, HullSize.CRUISER).stream().map(line -> line.text().plain()).toList();

        assertEquals("Low Tech Affinity: 10", lines.get(0));
        assertTrue(lines.contains("2% more hull points for every 5 Low Tech Affinity (4%)"), lines.toString());
        assertTrue(lines.contains("2% more armor for every 10 Low Tech Affinity (2%)"), lines.toString());
        assertTrue(lines.contains("1% more cargo capacity for every 5 Low Tech Affinity (2%)"), lines.toString());
        assertTrue(lines.contains("High Tech Affinity: 0"), lines.toString());
        assertEquals("Midline Affinity: 0", lines.get(lines.size() - 1));
        assertEquals(8, lines.size());
    }
}
