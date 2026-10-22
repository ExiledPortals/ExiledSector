package exiledsector.skills.loader;

import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillType;
import exiledsector.skills.layout.SkillNodeDecoration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WormholePairValidatorTest {

    private static SkillNode wormholeNode(String id, String pairedNodeId) {
        return wormholeNode(id, pairedNodeId, pairedNodeId == null ? List.of() : List.of(pairedNodeId));
    }

    private static SkillNode wormholeNode(String id, String pairedNodeId, List<String> connectedNodeIds) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", SkillTier.WORMHOLE)
                .effects(List.of())
                .build();
        return new SkillNode(id, type, connectedNodeIds, 0f, 0f, new SkillNodeDecoration(null, null, null, null, pairedNodeId));
    }

    private static SkillNode plainNode(String id) {
        SkillType type = new SkillType.Builder(id + "_type", id, "a.png", SkillTier.SMALL)
                .effects(List.of())
                .build();
        return new SkillNode(id, type, List.of(), 0f, 0f);
    }

    @Test
    void noIssuesForAProperlyPairedWormhole() {
        SkillNode a = wormholeNode("wormhole_a", "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", "wormhole_a");

        assertTrue(WormholePairValidator.findIssues(List.of(a, b)).isEmpty());
    }

    @Test
    void nonWormholeNodesAreIgnoredEvenWithoutAPair() {
        SkillNode plain = plainNode("armor_1");

        assertTrue(WormholePairValidator.findIssues(List.of(plain)).isEmpty());
    }

    @Test
    void flagsAWormholeWithNoPairedWithId() {
        SkillNode a = wormholeNode("wormhole_a", null);

        List<String> issues = WormholePairValidator.findIssues(List.of(a));

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("wormhole_a"));
        assertTrue(issues.get(0).contains("no pairedWith id"));
    }

    @Test
    void flagsAWormholePairedWithItself() {
        SkillNode a = wormholeNode("wormhole_a", "wormhole_a");

        List<String> issues = WormholePairValidator.findIssues(List.of(a));

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("paired with itself"));
    }

    @Test
    void flagsAWormholePairedWithANonexistentNode() {
        SkillNode a = wormholeNode("wormhole_a", "does_not_exist");

        List<String> issues = WormholePairValidator.findIssues(List.of(a));

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("does not exist"));
    }

    @Test
    void flagsAWormholePairedWithANonWormholeNode() {
        SkillNode a = wormholeNode("wormhole_a", "armor_1");
        SkillNode plain = plainNode("armor_1");

        List<String> issues = WormholePairValidator.findIssues(List.of(a, plain));

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("not a wormhole-tier node"));
    }

    @Test
    void flagsAsymmetricPairing() {
        SkillNode a = wormholeNode("wormhole_a", "wormhole_b");
        SkillNode b = wormholeNode("wormhole_b", "wormhole_c");
        SkillNode c = wormholeNode("wormhole_c", "wormhole_b");

        List<String> issues = WormholePairValidator.findIssues(List.of(a, b, c));

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("wormhole_a"));
        assertTrue(issues.get(0).contains("wormhole_b"));
    }

    @Test
    void flagsAWormholeThatDoesNotListItsPartnerInConnectedTo() {
        SkillNode a = wormholeNode("wormhole_a", "wormhole_b", List.of());
        SkillNode b = wormholeNode("wormhole_b", "wormhole_a");

        List<String> issues = WormholePairValidator.findIssues(List.of(a, b));

        assertEquals(1, issues.size());
        assertTrue(issues.get(0).contains("wormhole_a"));
        assertTrue(issues.get(0).contains("connectedTo"));
    }
}
