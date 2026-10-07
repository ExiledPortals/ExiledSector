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

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HullFrameworkCustodyTest {

    private static final HullFrameworkData CRUISER_FRAMEWORK = new HullFrameworkData(HullSize.CRUISER, SocketableRarity.RARE,
            List.of(SocketType.BRIDGE, SocketType.SHIELD_GENERATOR, SocketType.REACTOR), 4L);

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
        SocketableStore.clearNpcFrameworkPreviews();
        globalMock.close();
    }

    private static ShipProfile fit(ShieldType shieldType, int fighterBays) {
        return new ShipProfile(HullSize.CRUISER, shieldType, fighterBays, Set.of(), false, 1000f, shieldType == ShieldType.PHASE);
    }

    private ShipSkillData shipWith(String shipId, HullFramework framework, Socketable... slotItems) {
        ShipSkillData shipData = new ShipSkillData();
        assertTrue(HullFrameworks.install(shipData, framework, ships));
        for (int slotIndex = 0; slotIndex < slotItems.length; slotIndex++) {
            if (slotItems[slotIndex] != null) {
                assertTrue(HullFrameworks.socket(shipData, slotIndex, slotItems[slotIndex], ships), "slot " + slotIndex);
            }
        }
        ships.put(shipId, shipData);
        return shipData;
    }

    @Test
    void frameworksGetSequentialIdsAndSurviveASaveUnderAStableName() {
        HullFramework first = store.addFramework(CRUISER_FRAMEWORK);
        HullFramework second = store.addFramework(CRUISER_FRAMEWORK);
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        SocketableSaveAliases.register(xstream);

        String xml = xstream.toXML(store);
        SocketableStore loaded = (SocketableStore) xstream.fromXML(xml);

        assertEquals(List.of("framework_1", "framework_2"), List.of(first.id(), second.id()));
        assertTrue(xml.contains("<exiledSector.HullFramework>"), xml);
        HullFramework restored = loaded.findFramework("framework_2");
        assertEquals(CRUISER_FRAMEWORK, restored.data());
        assertEquals("framework_3", loaded.addFramework(CRUISER_FRAMEWORK).id());
    }

    @Test
    void aStoreSavedBeforeFrameworksStartsCountingFromOne() {
        XStream xstream = new XStream(new DomDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.addPermission(AnyTypePermission.ANY);
        SocketableSaveAliases.register(xstream);
        SocketableStore oldStore = (SocketableStore) xstream.fromXML("<exiledSector.SocketableStore><owned/><nextId>4</nextId></exiledSector.SocketableStore>");

        assertEquals(List.of(), oldStore.frameworks());
        assertEquals("framework_1", oldStore.addFramework(CRUISER_FRAMEWORK).id());
    }

    @Test
    void installingNeedsTheRightHullSizeAndEveryRestrictedSocketMet() {
        HullFramework framework = store.addFramework(CRUISER_FRAMEWORK);

        assertNull(HullFrameworks.installBlock(framework, HullSize.CRUISER, fit(ShieldType.FRONT, 0), null, "ship"));
        assertEquals(HullFrameworks.BlockReason.WRONG_HULL_SIZE,
                HullFrameworks.installBlock(framework, HullSize.DESTROYER, fit(ShieldType.FRONT, 0), null, "ship").reason());
        HullFrameworks.InstallBlock shieldless = HullFrameworks.installBlock(framework, HullSize.CRUISER, fit(ShieldType.NONE, 0), null, "ship");
        assertEquals(HullFrameworks.BlockReason.UNMET_REQUIREMENT, shieldless.reason());
        assertEquals(SocketType.SHIELD_GENERATOR, shieldless.socketType());
        assertEquals("req_shields", shieldless.requirementTag());
        assertEquals(HullFrameworks.BlockReason.INSTALLED_ELSEWHERE,
                HullFrameworks.installBlock(framework, HullSize.CRUISER, fit(ShieldType.FRONT, 0), "other", "ship").reason());
        assertNull(HullFrameworks.installBlock(framework, HullSize.CRUISER, fit(ShieldType.FRONT, 0), "ship", "ship"));
    }

    @Test
    void itemsOnlyGoIntoAFrameworkSocketOfTheirOwnTypeAndOnlyOnce() {
        HullFramework framework = store.addFramework(CRUISER_FRAMEWORK);
        Socketable officer = store.add(SocketableDefinitions.get("officer"), 1L);
        Socketable chip = store.add(SocketableDefinitions.get("chip"), 2L);
        ShipSkillData shipData = shipWith("ship", framework);

        assertFalse(HullFrameworks.socket(shipData, 2, officer, ships));
        assertFalse(HullFrameworks.socket(shipData, 0, chip, ships));
        assertFalse(HullFrameworks.socket(shipData, 5, officer, ships));
        assertTrue(HullFrameworks.socket(shipData, 0, officer, ships));
        ShipSkillData otherShip = shipWith("other", store.addFramework(CRUISER_FRAMEWORK));
        assertFalse(HullFrameworks.socket(otherShip, 0, officer, ships));
        assertTrue(SocketCustody.isInstalled(ships, officer));
        assertEquals(SocketCustody.Installation.inFramework("ship", 0), SocketCustody.installations(ships).get(officer.id()));
        assertEquals("ship", SocketCustody.frameworkShipId(ships, framework));
    }

    @Test
    void swappingAFrameworkFreesItsItems() {
        HullFramework framework = store.addFramework(CRUISER_FRAMEWORK);
        Socketable officer = store.add(SocketableDefinitions.get("officer"), 1L);
        ShipSkillData shipData = shipWith("ship", framework, officer);
        HullFramework replacement = store.addFramework(CRUISER_FRAMEWORK);

        assertTrue(HullFrameworks.install(shipData, replacement, ships));
        ShipSkillData otherShip = shipWith("other", store.addFramework(CRUISER_FRAMEWORK));
        assertFalse(HullFrameworks.install(otherShip, replacement, ships));

        assertEquals(replacement.id(), shipData.getInstalledFrameworkId());
        assertEquals(Map.of(), shipData.getFrameworkSocketedItems());
        assertFalse(SocketCustody.isInstalled(ships, officer));
        assertNull(SocketCustody.frameworkShipId(ships, framework));
        assertTrue(store.frameworks().containsAll(List.of(framework, replacement)));
    }

    @Test
    void aSoldShipReturnsItsFrameworkAndItemsToStorage() {
        HullFramework framework = store.addFramework(CRUISER_FRAMEWORK);
        Socketable officer = store.add(SocketableDefinitions.get("officer"), 1L);
        ShipSkillData shipData = shipWith("ship", framework, officer);

        SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertNull(shipData.getInstalledFrameworkId());
        assertEquals(Map.of(), shipData.getFrameworkSocketedItems());
        assertEquals(List.of(framework), store.frameworks());
        assertEquals(List.of(officer), store.owned());
    }

    @Test
    void aShipLostInBattleTakesItsFrameworkAndItemsWithIt() {
        HullFramework framework = store.addFramework(CRUISER_FRAMEWORK);
        Socketable officer = store.add(SocketableDefinitions.get("officer"), 1L);
        shipWith("ship", framework, officer);
        lost.add("ship");

        Set<String> lostForGood = SocketCustody.reconcile(ships, Set.of(), lost, store);

        assertEquals(Set.of("ship"), lostForGood);
        assertEquals(List.of(), store.frameworks());
        assertEquals(List.of(), store.owned());
        assertTrue(lost.isEmpty());
    }

    @Test
    void anOwnedShipKeepsItsFramework() {
        HullFramework framework = store.addFramework(CRUISER_FRAMEWORK);
        ShipSkillData shipData = shipWith("ship", framework);
        lost.add("ship");

        SocketCustody.reconcile(ships, Set.of("ship"), lost, store);

        assertEquals(framework.id(), shipData.getInstalledFrameworkId());
    }

    @Test
    void aShipWithOnlyAFrameworkIsNotBlankAndARespecKeepsIt() {
        HullFramework framework = store.addFramework(CRUISER_FRAMEWORK);
        ShipSkillData shipData = shipWith("ship", framework);

        assertFalse(shipData.isBlank());
        shipData.resetAllocations();
        assertEquals(framework.id(), shipData.getInstalledFrameworkId());
    }

    @Test
    void aTransposedItemThatNoLongerFitsItsFrameworkSocketLeavesIt() {
        HullFramework framework = store.addFramework(CRUISER_FRAMEWORK);
        Socketable officer = store.add(SocketableDefinitions.get("officer"), 1L);
        ShipSkillData shipData = shipWith("ship", framework, officer);
        Socketable transposed = new Socketable(officer.id(), "chip", 3L, List.of());
        store.replace(officer, transposed);

        assertTrue(SocketCustody.unsocketIfMisfit(ships, transposed));
        assertNull(shipData.getFrameworkSocketedItem(0));
        assertNotNull(store.find(officer.id()));
    }
}
