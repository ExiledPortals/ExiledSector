package exiledsector.ui.node;

import exiledsector.i18n.Catalogue;
import exiledsector.i18n.I18n;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class NodeSearchTest {

    private final NodeAllocator.Snapshot tree = snapshotHiding();

    private static NodeAllocator.Snapshot snapshotHiding(String... hiddenNodeIds) {
        return new NodeAllocator.Snapshot(mock(ShipSkillData.class), null, 0, Set.of(hiddenNodeIds), null, null);
    }

    private static SkillNode node(String id, String name) {
        SkillType type = new SkillType.Builder(id, name, "", SkillTier.SMALL).build();
        return new SkillNode(id + "_1", type, List.of(), 0f, 0f);
    }

    @Test
    void inAnotherLanguageSearchAlsoMatchesTheEnglishNameAndTheId() {
        I18n.install(new Catalogue("zh_CN", Map.of("skillType.armor.name", "重型装甲")));
        NodeSearch search = new NodeSearch();
        SkillNode armor = node("armor", "Heavy Armor");

        search.setQuery("重型");
        assertTrue(search.matches(armor, tree));
        search.setQuery("heavy");
        assertTrue(search.matches(armor, tree));
        search.setQuery("armor");
        assertTrue(search.matches(armor, tree));
        assertEquals("重型装甲", armor.getType().getDisplayName());
        assertEquals("Heavy Armor", armor.getType().getSourceName());
    }

    @Test
    void anEmptyQueryMatchesNothingAndDimsNothing() {
        NodeSearch search = new NodeSearch();
        SkillNode armor = node("armor", "Heavy Armor");

        assertFalse(search.isActive());
        assertFalse(search.matches(armor, tree));
        assertEquals(1f, search.nodeAlpha(armor, tree));
        assertEquals(1f, search.backgroundAlpha());
    }

    @Test
    void aSingleCharacterMatchesNamesContainingItIgnoringCase() {
        NodeSearch search = new NodeSearch();
        SkillNode armor = node("armor", "Heavy Armor");
        SkillNode flux = node("flux", "Flux Coil");

        search.setQuery("H");

        assertTrue(search.matches(armor, tree));
        assertFalse(search.matches(flux, tree));
        assertEquals(1f, search.nodeAlpha(armor, tree));
        assertEquals(NodeSearch.DIM_ALPHA, search.nodeAlpha(flux, tree));
        assertEquals(NodeSearch.DIM_ALPHA, search.backgroundAlpha());
    }

    @Test
    void aHiddenNodeNeverMatchesAndStaysDimmed() {
        NodeSearch search = new NodeSearch();
        SkillNode armor = node("armor", "Heavy Armor");
        NodeAllocator.Snapshot hidingArmor = snapshotHiding("armor_1");

        search.setQuery("armor");

        assertFalse(search.matches(armor, hidingArmor));
        assertEquals(NodeSearch.DIM_ALPHA, search.nodeAlpha(armor, hidingArmor));
    }

    @Test
    void connectorsStayBrightOnlyWhenBothEndsMatch() {
        NodeSearch search = new NodeSearch();
        SkillNode armor = node("armor", "Heavy Armor");
        SkillNode plating = node("plating", "Armor Plating");
        SkillNode flux = node("flux", "Flux Coil");

        search.setQuery("armor");

        assertEquals(1f, search.connectorAlpha(armor, plating, tree));
        assertEquals(NodeSearch.DIM_ALPHA, search.connectorAlpha(armor, flux, tree));
    }

    @Test
    void optionalNodesMatchOnAnyOfTheirOptionNames() {
        SkillType shields = new SkillType.Builder("shields", "Shield Upgrade", "", SkillTier.NOTABLE).build();
        SkillType optional = new SkillType.Builder("pick", "Pick One", "", SkillTier.NOTABLE)
                .optionalOptionIds(List.of("shields"))
                .build();
        SkillNode node = new SkillNode("pick_1", optional, List.of(), 0f, 0f);
        NodeSearch search = new NodeSearch();
        search.setQuery("shield");

        try (MockedStatic<SkillTree> skillTree = Mockito.mockStatic(SkillTree.class)) {
            skillTree.when(() -> SkillTree.getType("shields")).thenReturn(shields);

            assertTrue(search.matches(node, tree));
        }
    }
}
