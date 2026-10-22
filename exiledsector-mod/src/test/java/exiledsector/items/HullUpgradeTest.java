package exiledsector.items;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledlib.items.Item;
import exiledlib.items.ItemDefinitions;
import exiledlib.items.ItemStore;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.tags.ShipProfile;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullUpgradeTest {

    private final Map<String, ShipSkillData> ships = new LinkedHashMap<>();
    private final Set<String> lost = new HashSet<>();
    private MockedStatic<Global> globalMock;
    private ItemStore store;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        store = ItemStore.get();
        ItemDefinitions.register(new JSONArray()
                .put(ItemFixtures.row("officer", "bridge", "HULL_PERCENT:10:10"))
                .put(ItemFixtures.subroutineRow("chip")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        globalMock.close();
    }

    private static ShipProfile fit(ShieldType shieldType) {
        return new ShipProfile(HullSize.CRUISER, shieldType, 0, Set.of(), false, 1000f, shieldType == ShieldType.PHASE);
    }

    private ShipSkillData shipWithPoints(String shipId, int points) {
        ShipSkillData shipData = new ShipSkillData();
        store.addUpgrades(HullSize.CRUISER, points);
        for (int i = 0; i < points; i++) {
            assertTrue(HullUpgradeRules.installUpgrade(shipData, HullSize.CRUISER, store));
        }
        ships.put(shipId, shipData);
        return shipData;
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        ItemSaveAliases.register(xstream);
        return xstream;
    }

    @Test
    void upgradeCargoDataRoundTrips() {
        HullUpgradeData upgrade = new HullUpgradeData(HullSize.CAPITAL_SHIP);

        assertEquals(upgrade, HullUpgradeData.of(upgrade.toSpecialItem()));
        assertEquals(new HullUpgradeData(HullSize.CRUISER), HullUpgradeData.parse("CRUISER"));
        assertNull(HullUpgradeData.parse("FIGHTER"));
        assertNull(HullUpgradeData.parse(""));
        assertEquals("graphics/icons/hull_upgrades/hull_upgrade_capital_unique.png", upgrade.iconPath());
    }

    @Test
    void installingAnUpgradeUsesOneUpAndGrantsAPointUpToFour() {
        ShipSkillData shipData = new ShipSkillData();
        store.addUpgrades(HullSize.CRUISER, 6);
        store.addUpgrades(HullSize.FRIGATE, 1);

        for (int i = 0; i < HullUpgradeRules.MAX_POINTS; i++) {
            assertTrue(HullUpgradeRules.installUpgrade(shipData, HullSize.CRUISER, store));
        }

        assertFalse(HullUpgradeRules.installUpgrade(shipData, HullSize.CRUISER, store));
        assertEquals(4, shipData.getHullUpgradePoints());
        assertEquals(2, store.upgradeCount(HullSize.CRUISER));
        assertEquals(HullUpgradeRules.UpgradeBlock.AT_MAX_POINTS, HullUpgradeRules.upgradeBlock(shipData, 2));
        ShipSkillData destroyer = new ShipSkillData();
        assertFalse(HullUpgradeRules.installUpgrade(destroyer, HullSize.DESTROYER, store));
        assertEquals(HullUpgradeRules.UpgradeBlock.NONE_IN_STORAGE, HullUpgradeRules.upgradeBlock(destroyer, 0));
        assertFalse(shipData.isBlank());
    }

    @Test
    void pointsUnlockSocketTypesWithinTheShipsRestrictionsAndLockingRefundsThem() {
        ShipSkillData shipData = shipWithPoints("ship", 2);

        assertEquals(HullUpgradeRules.UnlockBlock.UNMET_REQUIREMENT, HullUpgradeRules.unlockBlock(shipData, ShipSection.SHIELD_GENERATOR, fit(ShieldType.NONE)));
        assertTrue(HullUpgradeRules.unlock(shipData, ShipSection.BRIDGE, fit(ShieldType.NONE)));
        assertEquals(HullUpgradeRules.UnlockBlock.ALREADY_UNLOCKED, HullUpgradeRules.unlockBlock(shipData, ShipSection.BRIDGE, fit(ShieldType.NONE)));
        assertTrue(HullUpgradeRules.unlock(shipData, ShipSection.SHIELD_GENERATOR, fit(ShieldType.FRONT)));
        assertEquals(HullUpgradeRules.UnlockBlock.NO_POINTS, HullUpgradeRules.unlockBlock(shipData, ShipSection.REACTOR, fit(ShieldType.FRONT)));

        Item officer = store.add(ItemDefinitions.get("officer"), 1L);
        assertTrue(HullUpgradeRules.socket(shipData, ShipSection.BRIDGE, officer, ships));
        assertTrue(HullUpgradeRules.lock(shipData, ShipSection.BRIDGE));

        assertEquals(1, shipData.getUnspentHullUpgradePoints());
        assertEquals(List.of("shield_generator"), shipData.getUnlockedSocketTypeIds());
        assertFalse(SocketCustody.isInstalled(ships, officer));
        assertEquals(List.of(officer), store.owned());
    }

    @Test
    void itemsOnlyGoIntoUnlockedSocketsOfTheirOwnTypeAndOnlyOnce() {
        ShipSkillData shipData = shipWithPoints("ship", 2);
        HullUpgradeRules.unlock(shipData, ShipSection.BRIDGE, fit(ShieldType.FRONT));
        Item officer = store.add(ItemDefinitions.get("officer"), 1L);
        Item chip = store.add(ItemDefinitions.get("chip"), 2L);

        assertFalse(HullUpgradeRules.socket(shipData, ShipSection.REACTOR, officer, ships));
        assertFalse(HullUpgradeRules.socket(shipData, ShipSection.BRIDGE, chip, ships));
        assertTrue(HullUpgradeRules.socket(shipData, ShipSection.BRIDGE, officer, ships));
        ShipSkillData otherShip = shipWithPoints("other", 1);
        HullUpgradeRules.unlock(otherShip, ShipSection.BRIDGE, fit(ShieldType.FRONT));
        assertFalse(HullUpgradeRules.socket(otherShip, ShipSection.BRIDGE, officer, ships));
        assertEquals(SocketCustody.Installation.inSection("ship", "bridge"), SocketCustody.installations(ships).get(officer.id()));
    }

    @Test
    void aSoldShipReturnsItsItemsAndALostShipTakesThemWithIt() {
        ShipSkillData sold = shipWithPoints("sold", 1);
        HullUpgradeRules.unlock(sold, ShipSection.BRIDGE, fit(ShieldType.FRONT));
        Item kept = store.add(ItemDefinitions.get("officer"), 1L);
        HullUpgradeRules.socket(sold, ShipSection.BRIDGE, kept, ships);
        ShipSkillData destroyed = shipWithPoints("destroyed", 1);
        HullUpgradeRules.unlock(destroyed, ShipSection.BRIDGE, fit(ShieldType.FRONT));
        Item gone = store.add(ItemDefinitions.get("officer"), 2L);
        HullUpgradeRules.socket(destroyed, ShipSection.BRIDGE, gone, ships);
        lost.add("destroyed");

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertEquals(Map.of(), sold.getSectionItems());
        assertEquals(List.of(kept), store.owned());
        assertEquals(1, sold.getHullUpgradePoints());
    }

    @Test
    void aRespecKeepsTheSectionAndATransposedMisfitLeavesItsSocket() {
        ShipSkillData shipData = shipWithPoints("ship", 1);
        HullUpgradeRules.unlock(shipData, ShipSection.BRIDGE, fit(ShieldType.FRONT));
        Item officer = store.add(ItemDefinitions.get("officer"), 1L);
        HullUpgradeRules.socket(shipData, ShipSection.BRIDGE, officer, ships);

        ShipTrees.resetAllocations(shipData);
        assertEquals(List.of("bridge"), shipData.getUnlockedSocketTypeIds());
        Item transposed = Item.withEffects(officer.id(), "chip", 3L, List.of());
        store.replace(officer, transposed);

        assertTrue(SocketCustody.unsocketIfMisfit(ships, transposed));
        assertNull(shipData.getSectionItem("bridge"));
        assertNotNull(store.find(officer.id()));
    }

    @Test
    void theUpgradeRevisionMovesOnlyWhenTheCountsChange() {
        int startRevision = store.upgradeRevision();

        store.addUpgrades(HullSize.FRIGATE, 0);
        assertFalse(store.takeUpgrade(HullSize.FRIGATE));
        assertEquals(startRevision, store.upgradeRevision());

        store.addUpgrades(HullSize.FRIGATE, 2);
        int afterAdding = store.upgradeRevision();
        assertNotEquals(startRevision, afterAdding);

        assertTrue(store.takeUpgrade(HullSize.FRIGATE));
        assertNotEquals(afterAdding, store.upgradeRevision());
    }

    @Test
    void upgradeCountsSurviveASave() {
        store.addUpgrades(HullSize.DESTROYER, 3);

        ItemStore loaded = (ItemStore) xstream().fromXML(xstream().toXML(store));

        assertEquals(3, loaded.upgradeCount(HullSize.DESTROYER));
        assertTrue(loaded.takeUpgrade(HullSize.DESTROYER));
        assertEquals(Map.of(HullSize.DESTROYER, 2), loaded.upgradeCounts());
    }

}
