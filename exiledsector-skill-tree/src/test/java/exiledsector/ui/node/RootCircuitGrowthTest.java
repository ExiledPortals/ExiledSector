package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RootCircuitGrowthTest {

    private static final SkillType ROOT_TYPE = new SkillType.Builder("root_low_tech", "Low Tech", "a.png", SkillTier.ROOT).build();
    private final SkillNode lowTech = new SkillNode("root_low_tech_1", ROOT_TYPE, List.of(), 100f, 0f);
    private final SkillNode highTech = new SkillNode("root_high_tech_1", ROOT_TYPE, List.of(), -100f, 0f);

    private static RootCircuit twoRadiusCircuit() throws Exception {
        return RootCircuit.parse(new JSONObject("{\"extentRadii\": 3, \"traces\": [{\"points\": [[1, 0], [3, 0]]}]}"));
    }

    @Test
    void theCircuitStaysHiddenWhileChoosingAndFlyingThenGrowsOutFromTheChosenRoot() throws Exception {
        RootCircuit circuit = twoRadiusCircuit();
        RootCircuitGrowth growth = new RootCircuitGrowth(() -> circuit);
        StartingRootChoice choice = StartingRootChoice.pending(List.of(lowTech, highTech));

        growth.advance(0.5f, choice);
        assertEquals(0f, growth.grownRadii(lowTech, choice));

        choice.choose(lowTech);
        growth.advance(0.5f, choice);
        assertEquals(0f, growth.grownRadii(lowTech, choice));

        choice.advance(StartingRootChoice.FLIGHT_SECONDS);
        growth.advance(0.01f, choice);
        assertEquals(0f, growth.grownRadii(lowTech, choice));

        growth.advance(1f, choice);
        assertEquals(RootCircuitGrowth.GROW_RADII_PER_SECOND, growth.grownRadii(lowTech, choice), 0.001f);
        assertEquals(0f, growth.grownRadii(highTech, choice));

        growth.advance(5f, choice);
        assertEquals(RootCircuitGrowth.FULLY_GROWN, growth.grownRadii(lowTech, choice));
    }

    @Test
    void aShipThatAlreadyHasItsRootShowsTheWholeCircuitWithoutReplayingTheGrowth() throws Exception {
        RootCircuit circuit = twoRadiusCircuit();
        RootCircuitGrowth growth = new RootCircuitGrowth(() -> circuit);
        StartingRootChoice choice = StartingRootChoice.alreadyChosen(lowTech);

        growth.advance(0.01f, choice);

        assertEquals(RootCircuitGrowth.FULLY_GROWN, growth.grownRadii(lowTech, choice));
    }
}
