package exiledsector.socketables;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullUpgradeTest {

    private final Map<String, ShipSkillData> ships = new LinkedHashMap<>();
    private final Set<String> lost = new HashSet<>();
    private MockedStatic<Global> globalMock;
    private SocketableStore store;

    @BeforeEach
    void setUp() throws Exception {
        Map<String, Object> persistentData = new HashMap<>();
        SectorAPI sector = mock(SectorAPI.class);
        when(sector.getPersistentData()).thenReturn(persistentData);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getSector).thenReturn(sector);
        store = SocketableStore.get();
        SocketableDefinitions.register(new JSONArray()
                .put(SocketableFixtures.row("officer", "bridge", "HULL_PERCENT:10:10"))
                .put(SocketableFixtures.row("chip", "subroutine", "HULL_PERCENT:10:10")));
    }

    @AfterEach
    void tearDown() {
        SocketableDefinitions.clear();
        globalMock.close();
    }

    private static ShipProfile fit(ShieldType shieldType) {
        return new ShipProfile(HullSize.CRUISER, shieldType, 0, Set.of(), false, 1000f, shieldType == ShieldType.PHASE);
    }

    private ShipSkillData shipWithPoints(String shipId, int points) {
        ShipSkillData shipData = new ShipSkillData();
        store.addUpgrades(HullSize.CRUISER, points);
        for (int i = 0; i < points; i++) {
            assertTrue(FrameworkSockets.installUpgrade(shipData, HullSize.CRUISER, store));
        }
        ships.put(shipId, shipData);
        return shipData;
    }

    private static XStream xstream() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        SocketableSaveAliases.register(xstream);
        return xstream;
    }

    @Test
    void upgradeCargoDataRoundTripsAndReadsTheOldFrameworkFormat() {
        HullUpgradeData upgrade = new HullUpgradeData(HullSize.CAPITAL_SHIP);

        assertEquals(upgrade, HullUpgradeData.of(upgrade.toSpecialItem()));
        assertEquals(new HullUpgradeData(HullSize.CRUISER), HullUpgradeData.parse("CRUISER/RARE/bridge+reactor/5"));
        assertNull(HullUpgradeData.parse("FIGHTER"));
        assertNull(HullUpgradeData.parse(""));
        assertEquals("graphics/icons/frameworks/framework_capital_unique.png", upgrade.iconPath());
    }

    @Test
    void installingAnUpgradeUsesOneUpAndGrantsAPointUpToFour() {
        ShipSkillData shipData = new ShipSkillData();
        store.addUpgrades(HullSize.CRUISER, 6);
        store.addUpgrades(HullSize.FRIGATE, 1);

        for (int i = 0; i < FrameworkSockets.MAX_POINTS; i++) {
            assertTrue(FrameworkSockets.installUpgrade(shipData, HullSize.CRUISER, store));
        }

        assertFalse(FrameworkSockets.installUpgrade(shipData, HullSize.CRUISER, store));
        assertEquals(4, shipData.getFrameworkPoints());
        assertEquals(2, store.upgradeCount(HullSize.CRUISER));
        assertEquals(FrameworkSockets.UpgradeBlock.AT_MAX_POINTS, FrameworkSockets.upgradeBlock(shipData, 2));
        ShipSkillData destroyer = new ShipSkillData();
        assertFalse(FrameworkSockets.installUpgrade(destroyer, HullSize.DESTROYER, store));
        assertEquals(FrameworkSockets.UpgradeBlock.NONE_IN_STORAGE, FrameworkSockets.upgradeBlock(destroyer, 0));
        assertFalse(shipData.isBlank());
    }

    @Test
    void pointsUnlockSocketTypesWithinTheShipsRestrictionsAndLockingRefundsThem() {
        ShipSkillData shipData = shipWithPoints("ship", 2);

        assertEquals(FrameworkSockets.UnlockBlock.UNMET_REQUIREMENT, FrameworkSockets.unlockBlock(shipData, SocketType.SHIELD_GENERATOR, fit(ShieldType.NONE)));
        assertTrue(FrameworkSockets.unlock(shipData, SocketType.BRIDGE, fit(ShieldType.NONE)));
        assertEquals(FrameworkSockets.UnlockBlock.ALREADY_UNLOCKED, FrameworkSockets.unlockBlock(shipData, SocketType.BRIDGE, fit(ShieldType.NONE)));
        assertTrue(FrameworkSockets.unlock(shipData, SocketType.SHIELD_GENERATOR, fit(ShieldType.FRONT)));
        assertEquals(FrameworkSockets.UnlockBlock.NO_POINTS, FrameworkSockets.unlockBlock(shipData, SocketType.REACTOR, fit(ShieldType.FRONT)));

        Socketable officer = store.add(SocketableDefinitions.get("officer"), 1L);
        assertTrue(FrameworkSockets.socket(shipData, SocketType.BRIDGE, officer, ships));
        assertTrue(FrameworkSockets.lock(shipData, SocketType.BRIDGE));

        assertEquals(1, shipData.getUnspentFrameworkPoints());
        assertEquals(List.of("shield_generator"), shipData.getUnlockedSocketTypeIds());
        assertFalse(SocketCustody.isInstalled(ships, officer));
        assertEquals(List.of(officer), store.owned());
    }

    @Test
    void itemsOnlyGoIntoUnlockedSocketsOfTheirOwnTypeAndOnlyOnce() {
        ShipSkillData shipData = shipWithPoints("ship", 2);
        FrameworkSockets.unlock(shipData, SocketType.BRIDGE, fit(ShieldType.FRONT));
        Socketable officer = store.add(SocketableDefinitions.get("officer"), 1L);
        Socketable chip = store.add(SocketableDefinitions.get("chip"), 2L);

        assertFalse(FrameworkSockets.socket(shipData, SocketType.REACTOR, officer, ships));
        assertFalse(FrameworkSockets.socket(shipData, SocketType.BRIDGE, chip, ships));
        assertTrue(FrameworkSockets.socket(shipData, SocketType.BRIDGE, officer, ships));
        ShipSkillData otherShip = shipWithPoints("other", 1);
        FrameworkSockets.unlock(otherShip, SocketType.BRIDGE, fit(ShieldType.FRONT));
        assertFalse(FrameworkSockets.socket(otherShip, SocketType.BRIDGE, officer, ships));
        assertEquals(SocketCustody.Installation.inFramework("ship", "bridge"), SocketCustody.installations(ships).get(officer.id()));
    }

    @Test
    void aSoldShipReturnsItsItemsAndALostShipTakesThemWithIt() {
        ShipSkillData sold = shipWithPoints("sold", 1);
        FrameworkSockets.unlock(sold, SocketType.BRIDGE, fit(ShieldType.FRONT));
        Socketable kept = store.add(SocketableDefinitions.get("officer"), 1L);
        FrameworkSockets.socket(sold, SocketType.BRIDGE, kept, ships);
        ShipSkillData destroyed = shipWithPoints("destroyed", 1);
        FrameworkSockets.unlock(destroyed, SocketType.BRIDGE, fit(ShieldType.FRONT));
        Socketable gone = store.add(SocketableDefinitions.get("officer"), 2L);
        FrameworkSockets.socket(destroyed, SocketType.BRIDGE, gone, ships);
        lost.add("destroyed");

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertEquals(Map.of(), sold.getFrameworkSocketedItems());
        assertEquals(List.of(kept), store.owned());
        assertEquals(1, sold.getFrameworkPoints());
    }

    @Test
    void aRespecKeepsTheFrameworkAndATransposedMisfitLeavesItsSocket() {
        ShipSkillData shipData = shipWithPoints("ship", 1);
        FrameworkSockets.unlock(shipData, SocketType.BRIDGE, fit(ShieldType.FRONT));
        Socketable officer = store.add(SocketableDefinitions.get("officer"), 1L);
        FrameworkSockets.socket(shipData, SocketType.BRIDGE, officer, ships);

        shipData.resetAllocations();
        assertEquals(List.of("bridge"), shipData.getUnlockedSocketTypeIds());
        Socketable transposed = new Socketable(officer.id(), "chip", 3L, List.of());
        store.replace(officer, transposed);

        assertTrue(SocketCustody.unsocketIfMisfit(ships, transposed));
        assertNull(shipData.getFrameworkSocketedItem("bridge"));
        assertNotNull(store.find(officer.id()));
    }

    @Test
    void upgradeCountsSurviveASave() {
        store.addUpgrades(HullSize.DESTROYER, 3);

        SocketableStore loaded = (SocketableStore) xstream().fromXML(xstream().toXML(store));

        assertEquals(3, loaded.upgradeCount(HullSize.DESTROYER));
        assertTrue(loaded.takeUpgrade(HullSize.DESTROYER));
        assertEquals(Map.of(HullSize.DESTROYER, 2), loaded.upgradeCounts());
    }

    @Test
    void frameworksFromEarlierBuildsBecomeUnlockedSocketsOrUpgrades() throws Exception {
        String savedStore = "<exiledSector.SocketableStore><owned/><nextId>1</nextId><frameworks>"
                + "<exiledSector.HullFramework><id>framework_1</id><hullSize>CRUISER</hullSize><rarity>RARE</rarity>"
                + "<socketTypeIds><string>bridge</string><string>reactor</string><string>engine_room</string></socketTypeIds><seed>4</seed>"
                + "</exiledSector.HullFramework>"
                + "<exiledSector.HullFramework><id>framework_2</id><hullSize>FRIGATE</hullSize><rarity>COMMON</rarity>"
                + "<socketTypeIds><string>bridge</string></socketTypeIds><seed>5</seed></exiledSector.HullFramework>"
                + "</frameworks><lastFrameworkId>2</lastFrameworkId></exiledSector.SocketableStore>";
        XStream gameLike = xstream();
        gameLike.ignoreUnknownElements();
        SocketableStore oldStore = (SocketableStore) gameLike.fromXML(savedStore);
        String savedShip = "<exiledsector.skills.ShipSkillData><installedFrameworkId>framework_1</installedFrameworkId>"
                + "<frameworkSocketedItems><entry><string>1</string><string>socketable_9</string></entry></frameworkSocketedItems>"
                + "</exiledsector.skills.ShipSkillData>";
        gameLike.allowTypes(new Class[]{ShipSkillData.class});
        ShipSkillData oldShip = (ShipSkillData) gameLike.fromXML(savedShip);

        assertTrue(oldStore.migrateLegacyFrameworks(Map.of("ship", oldShip)));

        assertEquals(List.of("bridge", "reactor", "engine_room"), oldShip.getUnlockedSocketTypeIds());
        assertEquals(3, oldShip.getFrameworkPoints());
        assertEquals(Map.of("reactor", "socketable_9"), oldShip.getFrameworkSocketedItems());
        assertEquals(Map.of(HullSize.FRIGATE, 1), oldStore.upgradeCounts());
        assertFalse(oldStore.migrateLegacyFrameworks(Map.of("ship", oldShip)));
    }

    @Test
    void npcFlagshipsRollTwoToFourDistinctSocketTypesTheirFitAllows() {
        Random random = new Random(3L);
        int framed = 0;
        int trials = 20000;
        int[] counts = new int[5];
        Set<SocketType> seen = EnumSet.noneOf(SocketType.class);
        for (int i = 0; i < trials; i++) {
            ShipSkillData shipData = new ShipSkillData();
            NpcSocketables.rollFramework(shipData, HullSize.CRUISER, () -> fit(ShieldType.NONE), 20, random);
            List<String> unlocked = shipData.getUnlockedSocketTypeIds();
            if (!unlocked.isEmpty()) {
                framed++;
                counts[unlocked.size()]++;
                assertEquals(unlocked.size(), new HashSet<>(unlocked).size());
                unlocked.forEach(socketTypeId -> seen.add(SocketType.byId(socketTypeId)));
            }
        }

        assertEquals(0.5, framed / (double) trials, 0.02);
        assertEquals(0.7, counts[2] / (double) framed, 0.02);
        assertEquals(0.2, counts[3] / (double) framed, 0.02);
        assertEquals(0.1, counts[4] / (double) framed, 0.02);
        assertFalse(seen.contains(SocketType.SHIELD_GENERATOR));
        assertFalse(seen.contains(SocketType.PHASE_COIL));
        assertFalse(seen.contains(SocketType.FLIGHT_DECK));
    }

    @Test
    void aKilledFlagshipWithAFrameworkDropsAnUpgradeOneTimeInTen() {
        ShipSkillData framed = new ShipSkillData();
        framed.grantUnlockedSocketType("bridge");
        Random random = new Random(8L);
        int drops = 0;
        for (int i = 0; i < 20000; i++) {
            if (NpcSocketables.rollUpgradeDrop(framed, HullSize.CAPITAL_SHIP, random) != null) {
                drops++;
            }
        }

        assertEquals(0.1, drops / 20000.0, 0.01);
        assertNull(NpcSocketables.rollUpgradeDrop(new ShipSkillData(), HullSize.CAPITAL_SHIP, new Random(1L)));
    }
}
