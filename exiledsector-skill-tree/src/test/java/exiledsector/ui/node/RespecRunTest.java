package exiledsector.ui.node;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.template.AutoAllocateRun;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespecRunTest {

    private static List<SkillNode> nodes(int count) {
        List<SkillNode> nodes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            SkillType type = new SkillType.Builder("t" + i, "t" + i, "a.png", SkillTier.SMALL).effects(List.of()).build();
            nodes.add(new SkillNode("n" + i, type, List.of(), 0f, 0f));
        }
        return nodes;
    }

    @Test
    void theFirstNodeGoesImmediatelyAndTheRestFollowAtAutoAllocatePace() {
        List<SkillNode> plan = nodes(3);
        RespecRun run = new RespecRun(plan);
        List<String> removed = new ArrayList<>();

        run.advance(0f, node -> removed.add(node.getId()));
        assertEquals(List.of("n0"), removed);

        run.advance(run.stepSeconds(), node -> removed.add(node.getId()));
        run.advance(run.stepSeconds(), node -> removed.add(node.getId()));

        assertEquals(List.of("n0", "n1", "n2"), removed);
        assertTrue(run.isFinished());
        assertEquals(AutoAllocateRun.stepSecondsFor(3), run.stepSeconds(), 1e-6f);
    }

    @Test
    void aNodeThatCannotBeRemovedStopsTheRun() {
        RespecRun run = new RespecRun(nodes(3));
        List<String> attempted = new ArrayList<>();

        run.advance(10f, node -> {
            attempted.add(node.getId());
            return !node.getId().equals("n1");
        });

        assertEquals(List.of("n0", "n1"), attempted);
        assertTrue(run.isFinished());
    }

    @Test
    void cancellingStopsBeforeTheNextNode() {
        RespecRun run = new RespecRun(nodes(3));
        List<String> removed = new ArrayList<>();
        run.advance(0f, node -> removed.add(node.getId()));

        run.cancel();
        run.advance(10f, node -> removed.add(node.getId()));

        assertEquals(List.of("n0"), removed);
        assertTrue(run.isFinished());
        assertFalse(new RespecRun(nodes(1)).isFinished());
    }
}
