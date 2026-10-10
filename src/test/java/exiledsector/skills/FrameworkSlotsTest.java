package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledsector.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.socketables.NpcSocketables;
import exiledsector.socketables.SocketType;
import exiledsector.socketables.SocketableDefinitions;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkSlotsTest {

    private ShipSkillData shipData;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SocketableDefinitions.register(new JSONArray()
                .put(definition("bridge_item", "bridge", "HULL_PERCENT:5:5"))
                .put(definition("emitter", "shield_generator", "HULL_PERCENT:10:10"))
                .put(definition("chip", "subroutine", "HULL_PERCENT:20:20")));
        shipData = new ShipSkillData();
        shipData.grantUnlockedSocketType("bridge");
        shipData.grantUnlockedSocketType("shield_generator");
        shipData.socketFrameworkItem("bridge", NpcSocketables.id("bridge_item", 1L));
        shipData.socketFrameworkItem("shield_generator", NpcSocketables.id("emitter", 2L));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        SocketableDefinitions.clear();
        NpcSocketables.clearCache();
    }

    private static JSONObject definition(String id, String kind, String prefixes) throws Exception {
        return new JSONObject().put("id", id).put("kind", kind).put("name", id).put("rarity", "10").put("prefixes", prefixes);
    }

    private static ShipProfile fit(ShieldType shieldType) {
        return new ShipProfile(HullSize.CRUISER, shieldType, 0, Set.of(), false, 1000f, false);
    }

    private static float hullPercent(EffectTotals totals) {
        return totals.groups().get(0).totals().getOrDefault(DefenseSkillEffect.HULL_PERCENT, 0f);
    }

    @Test
    void everySocketedFrameworkItemCountsOnceWhenItsSocketIsActive() {
        List<FrameworkSlots.Slot> slots = FrameworkSlots.of(shipData, () -> fit(ShieldType.FRONT));
        EffectTotals totals = EffectTotals.of(shipData, List.of(), HullSize.CRUISER, 1f, slots);

        assertEquals(2, slots.size());
        assertTrue(slots.stream().allMatch(FrameworkSlots.Slot::active));
        assertEquals(15f, hullPercent(totals), 1e-4f);
        assertEquals(2, totals.slotEffects().size());
    }

    @Test
    void aRestrictedSocketTheCurrentFitFailsGoesInactiveAndStopsItsItem() {
        List<FrameworkSlots.Slot> slots = FrameworkSlots.of(shipData, () -> fit(ShieldType.NONE));

        assertTrue(slots.get(0).active());
        assertFalse(slots.get(1).active());
        assertEquals("req_shields", slots.get(1).unmetRequirement());
        assertEquals(5f, hullPercent(EffectTotals.of(shipData, List.of(), HullSize.CRUISER, 1f, slots)), 1e-4f);
    }

    @Test
    void theFitIsOnlyWorkedOutForRestrictedSocketsAndAtMostOnce() {
        AtomicInteger fitRequests = new AtomicInteger();
        FrameworkSlots.of(shipData, () -> {
            fitRequests.incrementAndGet();
            return fit(ShieldType.FRONT);
        });
        shipData.lockSocketType("shield_generator");
        shipData.grantUnlockedSocketType("reactor");
        FrameworkSlots.of(shipData, () -> {
            fitRequests.incrementAndGet();
            return fit(ShieldType.FRONT);
        });

        assertEquals(1, fitRequests.get());
    }

    @Test
    void anItemOfTheWrongTypeInASocketDoesNothing() {
        shipData.socketFrameworkItem("bridge", NpcSocketables.id("chip", 3L));

        List<FrameworkSlots.Slot> slots = FrameworkSlots.of(shipData, () -> fit(ShieldType.FRONT));

        assertFalse(slots.get(0).appliesItem());
        assertEquals(10f, hullPercent(EffectTotals.of(shipData, List.of(), HullSize.CRUISER, 1f, slots)), 1e-4f);
    }

    @Test
    void theNpcBonusMultiplierScalesFrameworkItems() {
        shipData.markNpcBuild();
        List<FrameworkSlots.Slot> slots = FrameworkSlots.of(shipData, () -> fit(ShieldType.FRONT));

        assertEquals(30f, hullPercent(EffectTotals.of(shipData, List.of(), HullSize.CRUISER, 2f, slots)), 1e-4f);
    }

    @Test
    void aShipWithoutUnlockedSocketsHasNoSlots() {
        assertEquals(List.of(), FrameworkSlots.of(new ShipSkillData(), () -> fit(ShieldType.FRONT)));
    }
}
