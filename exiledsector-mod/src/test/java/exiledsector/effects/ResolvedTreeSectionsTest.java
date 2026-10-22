package exiledsector.effects;

import exiledsector.items.ShipSection;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.items.NpcItems;
import exiledsector.items.ItemDefinitions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResolvedTreeSectionsTest {

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        NpcItems.clearCache();
        ResolvedTree.clearCache();
    }

    @Test
    void sectionItemsApplyUnderTheirOwnSlotModIdsAndAFitChangeRebuildsTheTree() throws Exception {
        ItemDefinitions.register(new JSONArray().put(new JSONObject().put("id", "officer").put("section", "bridge").put("name", "Officer")
                .put("rarity", "10").put("prefixes", "HULL_PERCENT:5:5")));
        ShipSkillData shipData = new ShipSkillData();
        shipData.grantUnlockedSocketType("bridge");
        shipData.grantUnlockedSocketType("reactor");
        shipData.installInSection("bridge", NpcItems.id("officer", 1L));
        List<ShipSections.UnlockedSection> slots = ShipSections.of(shipData, () -> null);

        ResolvedTree resolvedTree = ResolvedTrees.of(shipData, HullSize.CRUISER, 1f, slots);

        List<ResolvedTree.EffectEntry> effectEntries = resolvedTree.entries().stream()
                .filter(ResolvedTree.EffectEntry.class::isInstance).map(ResolvedTree.EffectEntry.class::cast).toList();
        assertEquals(List.of(new ResolvedTree.EffectEntry(DefenseSkillEffect.HULL_PERCENT, "exiledSector_framework_bridge_HULL_PERCENT", 5f)),
                effectEntries);
        assertSame(resolvedTree, ResolvedTrees.of(shipData, HullSize.CRUISER, 1f, ShipSections.of(shipData, () -> null)));
        List<ShipSections.UnlockedSection> inactiveSlots = List.of(new ShipSections.UnlockedSection(ShipSection.BRIDGE, slots.get(0).item(), "req_test"),
                slots.get(1));
        assertNotSame(resolvedTree, ResolvedTrees.of(shipData, HullSize.CRUISER, 1f, inactiveSlots));
    }
}
