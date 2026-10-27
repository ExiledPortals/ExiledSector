package exiledsector.items;

import com.fs.starfarer.api.combat.ShieldAPI.ShieldType;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.security.AnyTypePermission;
import exiledlib.MockSector;
import exiledsector.modifiers.ModifierSlot;
import exiledsector.skills.ShipSkillData;
import exiledsector.skills.ShipTrees;
import exiledsector.skills.tags.ShipProfile;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class HullUpgradeTest {

    private final Map<String, ShipSkillData> ships = new LinkedHashMap<>();
    private final Set<String> lost = new HashSet<>();
    private MockSector mockSector;
    private ItemStore store;

    @BeforeEach
    void setUp() throws Exception {
        mockSector = new MockSector();
        store = ItemStore.get();
        ItemDefinitions.register(new JSONArray()
                .put(HullItemRows.row("officer", "bridge", "HULL_PERCENT:10:10"))
                .put(HullItemRows.row("warranty", "bridge", "HULL_PERCENT:1:1; TARGETED_HULL_ITEM_EFFECT_PERCENT:100:100").put("unique", "true"))
                .put(ItemFixtures.subroutineRow("chip")));
    }

    @AfterEach
    void tearDown() {
        ItemDefinitions.clear();
        mockSector.close();
    }

    private static ShipProfile fit(ShieldType shieldType) {
        return new ShipProfile(HullSize.CRUISER, shieldType, 0, Set.of(), false, 1000f, shieldType == ShieldType.PHASE);
    }

    private ShipSkillData shipWithPoints(String shipId, int points) {
        ShipSkillData shipData = new ShipSkillData();
        for (int shipPoints = HullUpgradePoints.STARTING_POINTS; shipPoints < points; shipPoints++) {
            RefitSchematic schematic = RefitSchematic.forShipPoints(shipPoints);
            store.addSchematics(schematic, 1);
            assertTrue(HullUpgradeRules.applySchematic(shipData, schematic, store));
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
    void schematicCargoDataRoundTripsAndOldHullUpgradesReadAsStandard() {
        for (RefitSchematic schematic : RefitSchematic.values()) {
            assertEquals(schematic, RefitSchematic.of(schematic.toSpecialItem()));
        }
        assertEquals(RefitSchematic.STANDARD, RefitSchematic.parse("CRUISER"));
        assertEquals(RefitSchematic.STANDARD, RefitSchematic.parse("CAPITAL_SHIP"));
        assertEquals(RefitSchematic.DOMAIN, RefitSchematic.parse("domain"));
        assertNull(RefitSchematic.parse("FIGHTER"));
        assertNull(RefitSchematic.parse(""));
        assertEquals("graphics/icons/hull_upgrades/refit_schematic_military.png", RefitSchematic.MILITARY.iconPath());
    }

    @Test
    void pointsUnlockSocketTypesWithinTheShipsRestrictionsAndLockingRefundsThem() {
        ShipSkillData shipData = shipWithPoints("ship", 2);

        assertEquals(HullUpgradeRules.UnlockBlock.UNMET_REQUIREMENT, HullUpgradeRules.unlockBlock(shipData, ShipSection.SHIELD_GENERATOR, fit(ShieldType.NONE)));
        assertTrue(HullUpgradeRules.unlock(shipData, ShipSection.BRIDGE, fit(ShieldType.NONE)));
        assertEquals(HullUpgradeRules.UnlockBlock.ALREADY_UNLOCKED, HullUpgradeRules.unlockBlock(shipData, ShipSection.BRIDGE, fit(ShieldType.NONE)));
        assertTrue(HullUpgradeRules.unlock(shipData, ShipSection.SHIELD_GENERATOR, fit(ShieldType.FRONT)));
        assertEquals(HullUpgradeRules.UnlockBlock.NO_POINTS, HullUpgradeRules.unlockBlock(shipData, ShipSection.REACTOR, fit(ShieldType.FRONT)));
        assertFalse(HullUpgradeRules.unlock(shipData, ShipSection.REACTOR, fit(ShieldType.FRONT)));

        Item officer = store.add(ItemDefinitions.get("officer"), 1L);
        assertTrue(HullUpgradeRules.socket(shipData, ShipSection.BRIDGE, officer, ships));
        assertTrue(HullUpgradeRules.lock(shipData, ShipSection.BRIDGE));

        assertEquals(1, HullUpgradePoints.unspent(shipData));
        assertEquals(List.of("shield_generator"), shipData.getUnlockedSocketTypeIds());
        assertFalse(SocketCustody.isInstalled(ships, officer));
        assertEquals(List.of(officer), store.owned());
    }

    @Test
    void aWarrantySavedBeforeItsTargetingModifierExistedGainsItOnLoad() {
        HullItem saved = new HullItem("old_warranty", "warranty", 7L,
                List.of(RolledModifier.single(ModifierSlot.PREFIX, new RolledEffect("HULL_PERCENT", 1f))));

        assertEquals(2, saved.explicits().size());
        assertTrue(saved.targetsAnotherItem());
        assertNotNull(saved.targetSection());
    }

    @Test
    void theTuningNanoswarmRerollsATargetingItemsTargetToAnotherSectionEvenWithFixedModifiers() {
        HullItem warranty = (HullItem) store.add(ItemDefinitions.get("warranty"), 7L);
        ShipSection firstTarget = warranty.targetSection();

        assertTrue(ItemCrafting.canRecalibrate(warranty, warranty.definition(), Set.of()));
        assertNotNull(ItemCrafting.recalibrate(warranty, new java.util.Random(3L)));

        assertNotEquals(firstTarget, warranty.targetSection());
        assertNotEquals(ShipSection.BRIDGE, warranty.targetSection());
        assertEquals(warranty.targetSection(), warranty.targetSection());
        assertTrue(warranty.implicitLines(true).size() >= 2);
        HullItem restored = new HullItem(new UnresolvedItem(warranty));
        assertEquals(warranty.targetSection(), restored.targetSection());
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
        assertEquals(1, HullUpgradePoints.of(sold));
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
    void theSchematicRevisionMovesOnlyWhenTheCountsChange() {
        int startRevision = store.schematicRevision();

        store.addSchematics(RefitSchematic.SCRAPPED, 0);
        assertFalse(store.takeSchematic(RefitSchematic.SCRAPPED));
        assertEquals(startRevision, store.schematicRevision());

        store.addSchematics(RefitSchematic.SCRAPPED, 2);
        int afterAdding = store.schematicRevision();
        assertNotEquals(startRevision, afterAdding);

        assertTrue(store.takeSchematic(RefitSchematic.SCRAPPED));
        assertNotEquals(afterAdding, store.schematicRevision());
    }

    @Test
    void schematicCountsAndAppliedPointsSurviveASave() {
        store.addSchematics(RefitSchematic.MILITARY, 3);
        ShipSkillData shipData = shipWithPoints("ship", 4);

        ItemStore loaded = (ItemStore) xstream().fromXML(xstream().toXML(store));
        ShipSkillData loadedShip = (ShipSkillData) xstream().fromXML(xstream().toXML(shipData));

        assertEquals(3, loaded.schematicCount(RefitSchematic.MILITARY));
        assertTrue(loaded.takeSchematic(RefitSchematic.MILITARY));
        assertEquals(Map.of(RefitSchematic.MILITARY, 2), loaded.schematicCounts());
        assertEquals(4, HullUpgradePoints.of(loadedShip));
    }
}
