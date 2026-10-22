package exiledsector.skills;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import exiledlib.skills.EffectTotals;
import exiledlib.skills.skilleffect.DefenseSkillEffect;
import exiledsector.skills.tags.ShipProfile;
import exiledsector.items.HullUpgradeEffectSource;
import exiledsector.items.NpcItems;
import exiledlib.items.ItemDefinitions;
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

class ShipSectionsTest {

    private ShipSkillData shipData;

    @BeforeEach
    void setUp() throws Exception {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        ItemDefinitions.register(new JSONArray()
                .put(definition("bridge_item", "bridge", "HULL_PERCENT:5:5"))
                .put(definition("emitter", "shield_generator", "HULL_PERCENT:10:10"))
                .put(definition("chip", "reactor", "HULL_PERCENT:20:20")));
        shipData = new ShipSkillData();
        shipData.grantUnlockedSocketType("bridge");
        shipData.grantUnlockedSocketType("shield_generator");
        shipData.installInSection("bridge", NpcItems.id("bridge_item", 1L));
        shipData.installInSection("shield_generator", NpcItems.id("emitter", 2L));
    }

    @AfterEach
    void tearDown() {
        SkillTree.clearNodes();
        SkillTree.clearTypes();
        ItemDefinitions.clear();
        NpcItems.clearCache();
    }

    private static JSONObject definition(String id, String kind, String prefixes) throws Exception {
        return new JSONObject().put("id", id).put("name", id).put("rarity", "10").put("prefixes", prefixes).put("section", kind);
    }

    private static ShipProfile fit(ShieldType shieldType) {
        return new ShipProfile(HullSize.CRUISER, shieldType, 0, Set.of(), false, 1000f, false);
    }

    private static float hullPercent(EffectTotals totals) {
        return totals.groups().get(0).totals().getOrDefault(DefenseSkillEffect.HULL_PERCENT, 0f);
    }

    @Test
    void everySocketedSectionItemCountsOnceWhenItsSocketIsActive() {
        List<ShipSections.UnlockedSection> slots = ShipSections.of(shipData, () -> fit(ShieldType.FRONT));
        EffectTotals totals = EffectTotals.of(HullUpgradeEffectSource.contributions(shipData, slots, HullSize.CRUISER), 1f);

        assertEquals(2, slots.size());
        assertTrue(slots.stream().allMatch(ShipSections.UnlockedSection::active));
        assertEquals(15f, hullPercent(totals), 1e-4f);
        assertEquals(2, totals.contributions().size());
    }

    @Test
    void aRestrictedSocketTheCurrentFitFailsGoesInactiveAndStopsItsItem() {
        List<ShipSections.UnlockedSection> slots = ShipSections.of(shipData, () -> fit(ShieldType.NONE));

        assertTrue(slots.get(0).active());
        assertFalse(slots.get(1).active());
        assertEquals("req_shields", slots.get(1).unmetRequirement());
        assertEquals(5f, hullPercent(EffectTotals.of(HullUpgradeEffectSource.contributions(shipData, slots, HullSize.CRUISER), 1f)), 1e-4f);
    }

    @Test
    void theFitIsOnlyWorkedOutForRestrictedSocketsAndAtMostOnce() {
        AtomicInteger fitRequests = new AtomicInteger();
        ShipSections.of(shipData, () -> {
            fitRequests.incrementAndGet();
            return fit(ShieldType.FRONT);
        });
        shipData.lockSocketType("shield_generator");
        shipData.grantUnlockedSocketType("reactor");
        ShipSections.of(shipData, () -> {
            fitRequests.incrementAndGet();
            return fit(ShieldType.FRONT);
        });

        assertEquals(1, fitRequests.get());
    }

    @Test
    void anItemOfTheWrongTypeInASocketDoesNothing() {
        shipData.installInSection("bridge", NpcItems.id("chip", 3L));

        List<ShipSections.UnlockedSection> slots = ShipSections.of(shipData, () -> fit(ShieldType.FRONT));

        assertFalse(slots.get(0).appliesItem());
        assertEquals(10f, hullPercent(EffectTotals.of(HullUpgradeEffectSource.contributions(shipData, slots, HullSize.CRUISER), 1f)), 1e-4f);
    }

    @Test
    void theNpcBonusMultiplierScalesSectionItems() {
        shipData.markNpcBuild();
        List<ShipSections.UnlockedSection> slots = ShipSections.of(shipData, () -> fit(ShieldType.FRONT));

        assertEquals(30f, hullPercent(EffectTotals.of(HullUpgradeEffectSource.contributions(shipData, slots, HullSize.CRUISER), 2f)), 1e-4f);
    }

    @Test
    void aShipWithoutUnlockedSocketsHasNoSlots() {
        assertEquals(List.of(), ShipSections.of(new ShipSkillData(), () -> fit(ShieldType.FRONT)));
    }
}
