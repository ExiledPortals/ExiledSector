package exiledsector.hullitems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.tags.ShipProfile;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
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
    private HullItemStore store;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        store = HullItemStore.get();
        HullItemDefinitions.register(new JSONArray()
                .put(HullItemFixtures.row("officer", "bridge", "HULL_PERCENT:10:10"))
                .put(HullItemFixtures.row("chip", "subroutine", "HULL_PERCENT:10:10")));
    }

    @AfterEach
    void tearDown() {
        HullItemDefinitions.clear();
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
        HullItemSaveAliases.register(xstream);
        return xstream;
    }

    @Test
    void upgradeCargoDataRoundTrips() {
        HullUpgradeData upgrade = new HullUpgradeData(HullSize.CAPITAL_SHIP);

        assertEquals(upgrade, HullUpgradeData.of(upgrade.toSpecialItem()));
        assertEquals(new HullUpgradeData(HullSize.CRUISER), HullUpgradeData.parse("CRUISER"));
        assertNull(HullUpgradeData.parse("FIGHTER"));
        assertNull(HullUpgradeData.parse(""));
        assertEquals("graphics/icons/frameworks/framework_capital_unique.png", upgrade.iconPath());
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

        HullItem officer = store.add(HullItemDefinitions.get("officer"), 1L);
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
        HullItem officer = store.add(HullItemDefinitions.get("officer"), 1L);
        HullItem chip = store.add(HullItemDefinitions.get("chip"), 2L);

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
        HullItem kept = store.add(HullItemDefinitions.get("officer"), 1L);
        HullUpgradeRules.socket(sold, ShipSection.BRIDGE, kept, ships);
        ShipSkillData destroyed = shipWithPoints("destroyed", 1);
        HullUpgradeRules.unlock(destroyed, ShipSection.BRIDGE, fit(ShieldType.FRONT));
        HullItem gone = store.add(HullItemDefinitions.get("officer"), 2L);
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
        HullItem officer = store.add(HullItemDefinitions.get("officer"), 1L);
        HullUpgradeRules.socket(shipData, ShipSection.BRIDGE, officer, ships);

        shipData.resetAllocations();
        assertEquals(List.of("bridge"), shipData.getUnlockedSocketTypeIds());
        HullItem transposed = new HullItem(officer.id(), "chip", 3L, List.of());
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

        HullItemStore loaded = (HullItemStore) xstream().fromXML(xstream().toXML(store));

        assertEquals(3, loaded.upgradeCount(HullSize.DESTROYER));
        assertTrue(loaded.takeUpgrade(HullSize.DESTROYER));
        assertEquals(Map.of(HullSize.DESTROYER, 2), loaded.upgradeCounts());
    }

    @Test
    void npcFlagshipsRollTwoToFourDistinctSocketTypesTheirFitAllows() {
        Random random = new Random(3L);
        int framed = 0;
        int trials = 20000;
        int[] counts = new int[5];
        Set<HullItemType> seen = EnumSet.noneOf(HullItemType.class);
        for (int i = 0; i < trials; i++) {
            ShipSkillData shipData = new ShipSkillData();
            NpcHullItems.rollSectionItems(shipData, HullSize.CRUISER, () -> fit(ShieldType.NONE), 20, random);
            List<String> unlocked = shipData.getUnlockedSocketTypeIds();
            if (!unlocked.isEmpty()) {
                framed++;
                counts[unlocked.size()]++;
                assertEquals(unlocked.size(), new HashSet<>(unlocked).size());
                unlocked.forEach(socketTypeId -> seen.add(HullItemType.byId(socketTypeId)));
            }
        }

        assertEquals(0.5, framed / (double) trials, 0.02);
        assertEquals(0.7, counts[2] / (double) framed, 0.02);
        assertEquals(0.2, counts[3] / (double) framed, 0.02);
        assertEquals(0.1, counts[4] / (double) framed, 0.02);
        assertFalse(seen.contains(HullItemType.SHIELD_GENERATOR));
        assertFalse(seen.contains(HullItemType.PHASE_COIL));
        assertFalse(seen.contains(HullItemType.FLIGHT_DECK));
    }

    @Test
    void aKilledFlagshipWithASectionDropsAnUpgradeOneTimeInTen() {
        ShipSkillData framed = new ShipSkillData();
        framed.grantUnlockedSocketType("bridge");
        Random random = new Random(8L);
        int drops = 0;
        for (int i = 0; i < 20000; i++) {
            if (NpcHullItems.rollUpgradeDrop(framed, HullSize.CAPITAL_SHIP, random) != null) {
                drops++;
            }
        }

        assertEquals(0.1, drops / 20000.0, 0.01);
        assertNull(NpcHullItems.rollUpgradeDrop(new ShipSkillData(), HullSize.CAPITAL_SHIP, new Random(1L)));
    }
}
