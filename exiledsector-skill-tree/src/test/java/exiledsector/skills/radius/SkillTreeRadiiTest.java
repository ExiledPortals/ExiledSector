package exiledsector.skills.radius;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.tags.SkillTags;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeRadiiTest {

    private static final SkillType HULL = new SkillType.Builder("hull", "Hull", "a.png", SkillTier.SMALL).build();
    private static final String TYPES = "{ \"skillTypes\": [ { \"id\": \"root_type\", \"name\": \"Root\", \"icon\": \"a.png\", \"tier\": \"ROOT\" },"
            + " { \"id\": \"hull\", \"name\": \"Hull\", \"icon\": \"a.png\" } ] }";
    private static final String NODES = "{ \"nodes\": ["
            + " { \"id\": \"root\", \"type\": \"root_type\", \"x\": 0, \"y\": 0 },"
            + " { \"id\": \"core_hull\", \"type\": \"hull\", \"connectedTo\": [\"root\"], \"x\": 100, \"y\": 0, \"tags\": [\"core\"] },"
            + " { \"id\": \"abyss_hull\", \"type\": \"hull\", \"connectedTo\": [\"root\"], \"x\": 0, \"y\": 100, \"tags\": [\"abyss\"] } ] }";

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        NodeRadii.register(new JSONObject("{ \"small\": 150, \"medium\": 250, \"large\": 350 }"));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clear();
        SkillTree.applyDisabledRegions(Set.of());
        NodeRadii.reset();
    }

    @Test
    void registeringAndUnregisteringNodesRebuildsTheRadii() {
        SkillTree.register(node("a", 0f, 0f));
        SkillTree.register(node("b", 100f, 0f));
        assertEquals(Set.of("b"), SkillTree.radii().nodeIdsWithin("a", RadiusSize.SMALL));

        SkillTree.unregister("b");
        assertEquals(Set.of(), SkillTree.radii().nodeIdsWithin("a", RadiusSize.SMALL));

        SkillTree.register(node("c", 0f, -120f));
        assertEquals(Set.of("c"), SkillTree.radii().nodeIdsWithin("a", RadiusSize.SMALL));
    }

    @Test
    void theCacheIsReusedUntilSomethingChanges() {
        SkillTree.register(node("a", 0f, 0f));

        assertSame(SkillTree.radii(), SkillTree.radii());
    }

    @Test
    void newDistancesFromDataRebuildTheRadii() throws Exception {
        SkillTree.register(node("a", 0f, 0f));
        SkillTree.register(node("b", 200f, 0f));
        assertEquals(Set.of(), SkillTree.radii().nodeIdsWithin("a", RadiusSize.SMALL));

        NodeRadii.register(new JSONObject("{ \"small\": 200, \"medium\": 250, \"large\": 350 }"));

        assertEquals(Set.of("b"), SkillTree.radii().nodeIdsWithin("a", RadiusSize.SMALL));
    }

    @Test
    void reloadingTheTreeAndTogglingRegionsRebuildsTheRadii() throws Exception {
        SettingsAPI settings = mock(SettingsAPI.class);
        when(settings.loadJSON("data/skilltrees/skill_types.json")).thenReturn(new JSONObject(TYPES));
        when(settings.loadJSON("data/skilltrees/ship_skill_tree.json")).thenReturn(new JSONObject(NODES));
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            global.when(Global::getSettings).thenReturn(settings);
            SkillTree.load();
            SkillTree.applyDisabledRegions(Set.of());
            NodeRadiusCache loaded = SkillTree.radii();
            assertEquals(Set.of("core_hull", "abyss_hull"), loaded.nodeIdsWithin("root", RadiusSize.SMALL));

            SkillTree.applyDisabledRegions(Set.of(SkillTags.ABYSS_REGION));
            assertEquals(Set.of("core_hull"), SkillTree.radii().nodeIdsWithin("root", RadiusSize.SMALL));
            assertEquals(Set.of(), SkillTree.radii().nodeIdsWithin("abyss_hull", RadiusSize.LARGE));

            SkillTree.applyDisabledRegions(Set.of());
            assertEquals(Set.of("core_hull", "abyss_hull"), SkillTree.radii().nodeIdsWithin("root", RadiusSize.SMALL));

            SkillTree.load();
            assertEquals(Set.of("core_hull", "abyss_hull"), SkillTree.radii().nodeIdsWithin("root", RadiusSize.SMALL));
        }
    }

    private static SkillNode node(String id, float treeX, float treeY) {
        return new SkillNode(id, HULL, List.of(), treeX, treeY);
    }
}
