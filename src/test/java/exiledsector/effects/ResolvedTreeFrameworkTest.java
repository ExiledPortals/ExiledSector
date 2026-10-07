package exiledsector.effects;

import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.FrameworkSlots;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.socketables.HullFrameworkData;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.SocketableDefinitions;
import exiledsector.socketables.SocketableRarity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ResolvedTreeFrameworkTest {

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
        NpcSocketables.clearCache();
        ResolvedTree.clearCache();
    }

    @Test
    void frameworkItemsApplyUnderTheirOwnSlotModIdsAndAFitChangeRebuildsTheTree() throws Exception {
        SocketableDefinitions.register(new JSONArray().put(new JSONObject().put("id", "officer").put("kind", "bridge").put("name", "Officer")
                .put("rarity", "10").put("prefixes", "HULL_PERCENT:5:5")));
        ShipSkillData shipData = new ShipSkillData();
        shipData.installFramework(new HullFrameworkData(HullSize.CRUISER, SocketableRarity.COMMON,
                List.of(SocketType.BRIDGE, SocketType.REACTOR), 1L).npcId());
        shipData.socketFrameworkItem(0, NpcSocketables.id("officer", 1L));
        List<FrameworkSlots.Slot> slots = FrameworkSlots.of(shipData, () -> null);

        ResolvedTree resolvedTree = ResolvedTree.of(shipData, HullSize.CRUISER, 1f, slots);

        List<ResolvedTree.EffectEntry> effectEntries = resolvedTree.entries().stream()
                .filter(ResolvedTree.EffectEntry.class::isInstance).map(ResolvedTree.EffectEntry.class::cast).toList();
        assertEquals(List.of(new ResolvedTree.EffectEntry(DefenseSkillEffect.HULL_PERCENT, "exiledSector_framework_0_HULL_PERCENT", 5f)),
                effectEntries);
        assertSame(resolvedTree, ResolvedTree.of(shipData, HullSize.CRUISER, 1f, FrameworkSlots.of(shipData, () -> null)));
        List<FrameworkSlots.Slot> inactiveSlots = List.of(new FrameworkSlots.Slot(0, SocketType.BRIDGE, slots.get(0).item(), "req_test"),
                slots.get(1));
        assertNotSame(resolvedTree, ResolvedTree.of(shipData, HullSize.CRUISER, 1f, inactiveSlots));
    }
}
