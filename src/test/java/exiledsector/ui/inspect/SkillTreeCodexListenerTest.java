package exiledsector.ui.inspect;

import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import exiledsector.i18n.Catalogue;
import exiledsector.i18n.I18n;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import exiledsector.skills.npc.NpcLayout;
import exiledsector.skills.npc.NpcLayouts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SkillTreeCodexListenerTest {

    private static final String NPC_TAG = "exiledSector_npcTree|bulwark|2|root_1,a_1";

    @BeforeEach
    void setUp() {
        CodexDataV2.ENTRIES.clear();
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SkillTree.register(new SkillNode("root_1", new SkillType.Builder("root", "Root", "a.png", SkillTier.ROOT).build(), List.of(), 0f, 0f));
        SkillTree.register(new SkillNode("a_1", new SkillType.Builder("a", "A", "a.png", SkillTier.SMALL).build(), List.of("root_1"), 0f, 0f));
    }

    @AfterEach
    void tearDown() {
        CodexDataV2.ENTRIES.clear();
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        NpcLayouts.register(Map.of());
    }

    private static FleetMemberAPI npc(String... tags) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.getTags()).thenReturn(List.of(tags));
        when(member.getVariant()).thenReturn(variant);
        return member;
    }

    private static CodexEntryPlugin memberEntry(String id, FleetMemberAPI member) {
        CodexEntryV2 entry = new CodexEntryV2(id, "Some ship", null, member);
        CodexDataV2.ENTRIES.put(id, entry);
        return entry;
    }

    private static List<CodexEntryPlugin> skillTreeEntries() {
        return CodexDataV2.ENTRIES.values().stream().filter(entry -> entry instanceof SkillTreeCodexEntry).toList();
    }

    @Test
    void openingTheCodexForALevelledNpcAddsALinkedSkillTreeEntry() {
        CodexEntryPlugin shipEntry = memberEntry("temp-1", npc(NPC_TAG));

        new SkillTreeCodexListener().reportAboutToOpenCodex();

        List<CodexEntryPlugin> added = skillTreeEntries();
        assertEquals(1, added.size());
        assertTrue(shipEntry.getRelatedEntries().contains(added.get(0)));
        assertTrue(added.get(0).getRelatedEntries().contains(shipEntry));
    }

    @Test
    void theBuildNameIsResolvedInTheGameLanguageEvenWhenTheSkillTreeUsesAnother() {
        NpcLayouts.register(Map.of("bulwark", new NpcLayout("bulwark", "Bulwark", "root_1", List.of(), "", List.of())));
        I18n.install(new Catalogue("zh_CN", Map.of("npcLayout.bulwark.name", "壁垒")), new Catalogue("en", Map.of()));
        memberEntry("temp-1", npc(NPC_TAG));

        new SkillTreeCodexListener().reportAboutToOpenCodex();

        SkillTreeCodexEntry added = (SkillTreeCodexEntry) skillTreeEntries().get(0);
        assertEquals("Bulwark", added.tree().layoutName());
    }

    @Test
    void shipsWithoutATreeGetNoExtraEntry() {
        memberEntry("temp-1", npc());

        new SkillTreeCodexListener().reportAboutToOpenCodex();

        assertTrue(skillTreeEntries().isEmpty());
    }

    @Test
    void reopeningTheCodexDoesNotDuplicateTheEntry() {
        memberEntry("temp-1", npc(NPC_TAG));
        SkillTreeCodexListener listener = new SkillTreeCodexListener();

        listener.reportAboutToOpenCodex();
        listener.reportAboutToOpenCodex();

        assertEquals(1, skillTreeEntries().size());
    }

    @Test
    void closingTheCodexRemovesAndUnlinksTheAddedEntries() {
        CodexEntryPlugin shipEntry = memberEntry("temp-1", npc(NPC_TAG));
        SkillTreeCodexListener listener = new SkillTreeCodexListener();
        listener.reportAboutToOpenCodex();

        listener.reportClosedCodex();

        assertTrue(skillTreeEntries().isEmpty());
        assertFalse(shipEntry.getRelatedEntries().stream().anyMatch(entry -> entry instanceof SkillTreeCodexEntry));
    }
}
