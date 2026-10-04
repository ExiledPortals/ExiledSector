package exiledsector.ui.inspect;

import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.codex.CodexDataV2;
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin;
import com.fs.starfarer.api.impl.codex.CodexEntryV2;
import exiledsector.i18n.Catalogue;
import exiledsector.i18n.I18n;
import exiledsector.skills.PhantomHullModStatus;
import exiledsector.skills.SkillDataResolver;
import exiledsector.skills.SkillNode;
import exiledsector.skills.SkillTier;
import exiledsector.skills.SkillTree;
import exiledsector.skills.SkillType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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
        SkillTree.register(new SkillNode("a_1", new SkillType.Builder("a", "A", "a.png", SkillTier.SMALL).tags(List.of("ballistic")).build(), List.of("root_1"), 0f, 0f));
    }

    @AfterEach
    void tearDown() {
        CodexDataV2.ENTRIES.clear();
        SkillDataResolver.clearCache();
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        PhantomHullModStatus.clear();
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
        I18n.install(new Catalogue("zh_CN", Map.of("theme.ballistic", "实弹")), new Catalogue("en", Map.of("theme.ballistic", "Ballistic")));
        memberEntry("temp-1", npc(NPC_TAG));

        new SkillTreeCodexListener().reportAboutToOpenCodex();

        SkillTreeCodexEntry added = (SkillTreeCodexEntry) skillTreeEntries().get(0);
        assertEquals(List.of("ballistic"), added.tree().buildThemes());
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

    private static CodexEntryPlugin hullModEntry(String hullModId) {
        String id = CodexDataV2.getHullmodEntryId(hullModId);
        CodexEntryV2 entry = new CodexEntryV2(id, hullModId, null, null);
        CodexDataV2.ENTRIES.put(id, entry);
        return entry;
    }

    private static FleetMemberAPI shipWithPhantom(String phantomId, String... otherHullMods) {
        FleetMemberAPI member = npc();
        ShipVariantAPI variant = member.getVariant();
        List<String> hullMods = new ArrayList<>(List.of(otherHullMods));
        hullMods.add(phantomId);
        when(variant.getHullMods()).thenReturn(hullMods);
        when(variant.hasTag("exiledSector_installed_" + phantomId)).thenReturn(true);
        return member;
    }

    private static void link(CodexEntryPlugin a, CodexEntryPlugin b) {
        a.addRelatedEntry(b);
        b.addRelatedEntry(a);
    }

    @Test
    void aPhantomHullModIsNoLongerARelatedEntryOfTheShipItWasPlacedOn() {
        PhantomHullModStatus.markActive("safetyoverrides");
        FleetMemberAPI member = shipWithPhantom("safetyoverrides", "eccm");
        CodexEntryPlugin shipEntry = memberEntry("temp-1", member);
        CodexEntryPlugin safetyOverrides = hullModEntry("safetyoverrides");
        CodexEntryPlugin eccm = hullModEntry("eccm");
        link(shipEntry, safetyOverrides);
        link(shipEntry, eccm);

        new SkillTreeCodexListener().reportAboutToOpenCodex();

        assertFalse(shipEntry.getRelatedEntries().contains(safetyOverrides));
        assertFalse(safetyOverrides.getRelatedEntries().contains(shipEntry));
        assertTrue(shipEntry.getRelatedEntries().contains(eccm));
        assertTrue(eccm.getRelatedEntries().contains(shipEntry));
    }

    @Test
    void aHullModTheHullItselfRelatesToKeepsItsLink() {
        PhantomHullModStatus.markActive("heavyarmor");
        FleetMemberAPI member = shipWithPhantom("heavyarmor");
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        when(hullSpec.isDefaultDHull()).thenReturn(false);
        when(hullSpec.getHullId()).thenReturn("onslaught");
        when(member.getHullSpec()).thenReturn(hullSpec);
        CodexEntryPlugin heavyArmor = hullModEntry("heavyarmor");
        String hullEntryId = CodexDataV2.getFleetMemberEntryId(member);
        CodexEntryV2 hullEntry = new CodexEntryV2(hullEntryId, "Onslaught", null, hullSpec);
        CodexDataV2.ENTRIES.put(hullEntryId, hullEntry);
        hullEntry.addRelatedEntry(heavyArmor);
        CodexEntryPlugin shipEntry = memberEntry("temp-1", member);
        link(shipEntry, heavyArmor);

        new SkillTreeCodexListener().reportAboutToOpenCodex();

        assertTrue(shipEntry.getRelatedEntries().contains(heavyArmor));
    }

    @Test
    void aTaggedHullModThatIsNotAnActivePhantomKeepsItsLink() {
        FleetMemberAPI member = shipWithPhantom("safetyoverrides");
        CodexEntryPlugin shipEntry = memberEntry("temp-1", member);
        CodexEntryPlugin safetyOverrides = hullModEntry("safetyoverrides");
        link(shipEntry, safetyOverrides);

        new SkillTreeCodexListener().reportAboutToOpenCodex();

        assertTrue(shipEntry.getRelatedEntries().contains(safetyOverrides));
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
