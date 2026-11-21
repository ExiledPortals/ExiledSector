package exiledsector.items.pattern;

import com.fs.starfarer.api.campaign.FleetDataAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import exiledsector.MockSector;
import exiledsector.items.HullItemRows;
import exiledsector.items.Item;
import exiledsector.items.ItemDefinitions;
import exiledsector.items.ItemStore;
import exiledsector.items.crafting.material.CraftingMaterials;
import exiledsector.items.custody.HullPatternCustody;
import exiledsector.items.hull.ShipSection;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSections;
import exiledsector.skills.ShipSkillData;
import lunalib.lunaSettings.LunaSettings;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullPatternsTest {

    private static final String HULL_TYPE = "hammerhead";
    private static final String UNIQUE = "test_unique_bridge";

    private MockSector mockSector;
    private MockedStatic<LunaSettings> lunaSettingsMock;
    private final List<FleetMemberAPI> fleetMembers = new ArrayList<>();
    private final Map<String, Integer> feedstock = new HashMap<>();
    private CraftingMaterials wallet;
    private FleetMemberAPI patternShip;
    private FleetMemberAPI firstFollower;
    private FleetMemberAPI secondFollower;
    private Item bridgeItem;
    private Item engineItem;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        lunaSettingsMock = Mockito.mockStatic(LunaSettings.class, invocation -> null);
        FleetDataAPI fleetData = mock(FleetDataAPI.class);
        when(mockSector.playerFleet().getFleetData()).thenReturn(fleetData);
        when(fleetData.getMembersListCopy()).thenAnswer(invocation -> new ArrayList<>(fleetMembers));
        ItemDefinitions.register(new JSONArray()
                .put(HullItemRows.row(HullItemRows.MILITARY, HullItemRows.MILITARY_SECTION, HullItemRows.MILITARY_PREFIXES))
                .put(HullItemRows.row("test_engine", "engine_room", HullItemRows.MILITARY_PREFIXES))
                .put(HullItemRows.row(UNIQUE, "bridge", HullItemRows.MILITARY_PREFIXES).put("unique", "true")));
        bridgeItem = ItemStore.get().add(ItemDefinitions.get(HullItemRows.MILITARY), 1L);
        engineItem = ItemStore.get().add(ItemDefinitions.get("test_engine"), 2L);
        patternShip = ship("pattern", HULL_TYPE);
        firstFollower = ship("first", HULL_TYPE);
        secondFollower = ship("second", HULL_TYPE);
        ShipSkillData patternData = unlocked("pattern", "bridge", "engine_room");
        patternData.installInSection("bridge", bridgeItem.id());
        patternData.installInSection("engine_room", engineItem.id());
        unlocked("first", "bridge", "engine_room");
        unlocked("second", "bridge");
        feedstock.put(HullPatternRules.FEEDSTOCK_ID, 100_000);
        wallet = CraftingMaterials.storedForTests(feedstock);
        HullPatternActivity.reset();
        ShipSections.clearCache();
    }

    @AfterEach
    void tearDown() {
        HullPatternActivity.reset();
        ShipSections.clearCache();
        ItemDefinitions.clear();
        lunaSettingsMock.close();
        mockSector.close();
    }

    private FleetMemberAPI ship(String id, String hullType) {
        FleetMemberAPI member = mock(FleetMemberAPI.class);
        ShipHullSpecAPI hullSpec = mock(ShipHullSpecAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(hullSpec.getBaseHullId()).thenReturn(hullType);
        when(hullSpec.getHullId()).thenReturn(hullType);
        when(variant.getHullSpec()).thenReturn(hullSpec);
        when(variant.getHullMods()).thenReturn(List.of());
        when(member.getId()).thenReturn(id);
        when(member.getShipName()).thenReturn("ISS " + id);
        when(member.getHullSpec()).thenReturn(hullSpec);
        when(member.getVariant()).thenReturn(variant);
        fleetMembers.add(member);
        return member;
    }

    private static ShipSkillData unlocked(String shipId, String... sectionIds) {
        ShipSkillData shipData = ShipSkillDataManager.get(shipId);
        for (String sectionId : sectionIds) {
            shipData.grantUnlockedSection(sectionId);
        }
        return shipData;
    }

    private static ShipSections.UnlockedSection section(String shipId, ShipSection section) {
        for (ShipSections.UnlockedSection unlocked : sectionsOf(shipId)) {
            if (unlocked.section() == section) {
                return unlocked;
            }
        }
        return null;
    }

    private static List<ShipSections.UnlockedSection> sectionsOf(String shipId) {
        return ShipSections.forVariant(ShipSkillDataManager.find(shipId), HullPatternActivity.fleetMember(shipId).getVariant());
    }

    private int feedstockLeft() {
        return wallet.count(HullPatternRules.FEEDSTOCK_ID);
    }

    private HullPattern patternWithBridgeSelected() {
        HullPattern pattern = HullPatternRules.create(patternShip, true);
        assertTrue(HullPatternRules.select(pattern, section("pattern", ShipSection.BRIDGE), wallet));
        return pattern;
    }

    @Test
    void aLinkedShipClonesThePatternItemOnlyInSelectedSocketsItHasUnlocked() {
        HullPattern pattern = patternWithBridgeSelected();
        feedstock.put(HullPatternRules.FEEDSTOCK_ID, 1000);
        assertTrue(HullPatternRules.link(pattern, firstFollower, true, wallet));

        ShipSections.UnlockedSection bridge = section("first", ShipSection.BRIDGE);
        assertSame(bridgeItem, bridge.item());
        assertTrue(bridge.cloned());
        assertNull(section("first", ShipSection.ENGINE_ROOM).item());
        assertEquals(500, feedstockLeft());
    }

    @Test
    void aShipsOwnItemOverridesTheClone() {
        HullPattern pattern = patternWithBridgeSelected();
        feedstock.put(HullPatternRules.FEEDSTOCK_ID, 500);
        HullPatternRules.link(pattern, firstFollower, true, wallet);
        Item ownItem = ItemStore.get().add(ItemDefinitions.get(HullItemRows.MILITARY), 9L);

        ShipSkillDataManager.find("first").installInSection("bridge", ownItem.id());

        ShipSections.UnlockedSection bridge = section("first", ShipSection.BRIDGE);
        assertSame(ownItem, bridge.item());
        assertFalse(bridge.cloned());
    }

    @Test
    void costsAreFiveHundredPerSocketPerLinkedShipAndNothingIsTakenWhenShort() {
        HullPattern pattern = patternWithBridgeSelected();
        feedstock.put(HullPatternRules.FEEDSTOCK_ID, 1000);
        HullPatternRules.link(pattern, firstFollower, true, wallet);
        HullPatternRules.link(pattern, secondFollower, true, wallet);
        assertEquals(0, feedstockLeft());

        feedstock.put(HullPatternRules.FEEDSTOCK_ID, 999);
        ShipSections.UnlockedSection engine = section("pattern", ShipSection.ENGINE_ROOM);
        assertEquals(1000, HullPatternRules.selectCost(pattern));
        assertEquals(HullPatternRules.SelectBlock.CANNOT_AFFORD, HullPatternRules.selectBlock(pattern, engine, 999));
        assertFalse(HullPatternRules.select(pattern, engine, wallet));
        assertEquals(999, feedstockLeft());

        feedstock.put(HullPatternRules.FEEDSTOCK_ID, 1000);
        assertTrue(HullPatternRules.select(pattern, engine, wallet));
        assertEquals(0, feedstockLeft());
        assertEquals(1000, HullPatternRules.linkCost(pattern));
    }

    @Test
    void eachHullTypeHasOnlyOnePattern() {
        assertNotNull(HullPatternRules.create(patternShip, true));

        assertEquals(HullPatternRules.CreateBlock.PATTERN_EXISTS, HullPatternRules.createBlock(HullPatterns.get(), firstFollower, true));
        assertNull(HullPatternRules.create(firstFollower, true));
        assertEquals(1, HullPatterns.get().all().size());
    }

    @Test
    void stationsAndOtherHullTypesCannotJoinAPattern() {
        HullPattern pattern = HullPatternRules.create(patternShip, true);
        FleetMemberAPI station = ship("station", HULL_TYPE);
        when(station.isStation()).thenReturn(true);
        FleetMemberAPI otherHull = ship("other", "lasher");

        assertEquals(HullPatternRules.CreateBlock.NOT_PATTERNABLE, HullPatternRules.createBlock(HullPatterns.get(), station, true));
        assertEquals(HullPatternRules.LinkBlock.NOT_PATTERNABLE, HullPatternRules.linkBlock(pattern, station, true, 0));
        assertEquals(HullPatternRules.LinkBlock.OTHER_HULL_TYPE, HullPatternRules.linkBlock(pattern, otherHull, true, 0));
        assertEquals(HullPatternRules.LinkBlock.PATTERN_SHIP, HullPatternRules.linkBlock(pattern, patternShip, true, 0));
    }

    @Test
    void uniquesAreNeverClonedAndTheirSocketCannotBeSelected() {
        Item unique = ItemStore.get().add(ItemDefinitions.get(UNIQUE), 3L);
        ShipSkillDataManager.find("pattern").installInSection("bridge", unique.id());
        HullPattern pattern = HullPatternRules.create(patternShip, true);

        assertEquals(HullPatternRules.SelectBlock.HOLDS_UNIQUE, HullPatternRules.selectBlock(pattern, section("pattern", ShipSection.BRIDGE), 0));

        HullPatternRules.select(pattern, section("pattern", ShipSection.ENGINE_ROOM), wallet);
        HullPatternRules.link(pattern, firstFollower, true, wallet);
        pattern.select(ShipSection.BRIDGE);
        HullPatterns.markChanged();
        assertNull(section("first", ShipSection.BRIDGE).item());
        assertSame(engineItem, section("first", ShipSection.ENGINE_ROOM).item());
    }

    @Test
    void aPatternClonesAtMostFiveSockets() {
        HullPattern pattern = HullPatternRules.create(patternShip, true);
        List<ShipSection> sections = ShipSection.all();
        for (int index = 0; index < HullPatternRules.MAX_SOCKETS; index++) {
            pattern.select(sections.get(index));
        }
        ShipSections.UnlockedSection sixth = new ShipSections.UnlockedSection(sections.get(HullPatternRules.MAX_SOCKETS), null, null);

        assertEquals(HullPatternRules.SelectBlock.AT_LIMIT, HullPatternRules.selectBlock(pattern, sixth, 0));
    }

    @Test
    void aSocketThePatternShipCannotUseCannotBePaidFor() {
        HullPattern pattern = HullPatternRules.create(patternShip, true);
        ShipSections.UnlockedSection inactive = new ShipSections.UnlockedSection(ShipSection.SHIELD_GENERATOR, null, "req_shields");

        assertEquals(HullPatternRules.SelectBlock.REQUIREMENT_UNMET, HullPatternRules.selectBlock(pattern, inactive, 10_000));
        assertEquals(HullPatternRules.SelectBlock.SECTION_LOCKED, HullPatternRules.selectBlock(pattern, null, 10_000));
    }

    @Test
    void aPatternShipOutOfTheFleetStopsCloningButKeepsThePattern() {
        HullPattern pattern = patternWithBridgeSelected();
        HullPatternRules.link(pattern, firstFollower, true, wallet);
        assertSame(bridgeItem, section("first", ShipSection.BRIDGE).item());

        fleetMembers.remove(patternShip);
        HullPatternActivity.playerFleetSynced();

        assertNull(section("first", ShipSection.BRIDGE).item());
        assertSame(pattern, HullPatterns.get().forHullType(HULL_TYPE));

        fleetMembers.add(patternShip);
        HullPatternActivity.playerFleetSynced();
        assertSame(bridgeItem, section("first", ShipSection.BRIDGE).item());
    }

    @Test
    void aFollowerThatLeavesStaysLinkedAndClonesAgainOnReturn() {
        HullPattern pattern = patternWithBridgeSelected();
        HullPatternRules.link(pattern, firstFollower, true, wallet);

        fleetMembers.remove(firstFollower);
        HullPatternActivity.playerFleetSynced();
        assertTrue(pattern.isFollower("first"));

        fleetMembers.add(firstFollower);
        HullPatternActivity.playerFleetSynced();
        assertSame(bridgeItem, section("first", ShipSection.BRIDGE).item());
    }

    @Test
    void losingThePatternShipDeletesThePatternAndLeavesItsItems() {
        HullPattern pattern = patternWithBridgeSelected();
        HullPatternRules.link(pattern, firstFollower, true, wallet);

        HullPatternCustody.INSTANCE.release("pattern", ShipSkillDataManager.find("pattern"), false, ItemStore.get());

        assertTrue(HullPatterns.get().isEmpty());
        assertTrue(ItemStore.get().owned().contains(bridgeItem));
    }

    @Test
    void aDestroyedFollowerCostsThePatternShipOneOfTheItemsItWasCloning() {
        HullPattern pattern = patternWithBridgeSelected();
        HullPatternRules.link(pattern, firstFollower, true, wallet);
        HullPatternRules.link(pattern, secondFollower, true, wallet);

        Item lost = HullPatternLosses.followerDestroyed(pattern, "second", ShipSkillDataManager.find("second"), ItemStore.get(), new Random(4L));

        assertSame(bridgeItem, lost);
        assertFalse(ItemStore.get().owned().contains(bridgeItem));
        assertNull(ShipSkillDataManager.find("pattern").getSectionItem("bridge"));
        assertSame(engineItem, ItemStore.lookup(ShipSkillDataManager.find("pattern").getSectionItem("engine_room")));
        assertFalse(pattern.isFollower("second"));
        assertTrue(pattern.isFollower("first"));
    }

    @Test
    void aDestroyedFollowerThatClonedNothingCostsNothing() {
        HullPattern pattern = HullPatternRules.create(patternShip, true);
        HullPatternRules.select(pattern, section("pattern", ShipSection.ENGINE_ROOM), wallet);
        HullPatternRules.link(pattern, secondFollower, true, wallet);

        assertNull(HullPatternLosses.followerDestroyed(pattern, "second", ShipSkillDataManager.find("second"), ItemStore.get(), new Random(4L)));
        assertTrue(ItemStore.get().owned().contains(engineItem));
    }
}
