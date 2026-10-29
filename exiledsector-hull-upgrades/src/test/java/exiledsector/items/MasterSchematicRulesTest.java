package exiledsector.items;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import exiledsector.persistence.ShipSkillDataManager;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MasterSchematicRulesTest {

    private final ItemStore store = new ItemStore();
    private MockSector mockSector;
    private final Map<String, Object> sectorMemory = new HashMap<>();

    @BeforeEach
    void setUp() {
        MasterSchematic.requiredWhen(HullUpgradesSetting::masterSchematicsRequired);
        HullUpgradesSetting.setMasterSchematicsRequiredForTests(true);
        mockSector = new MockSector();
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.getBoolean(anyString())).thenAnswer(invocation -> Boolean.TRUE.equals(sectorMemory.get((String) invocation.getArgument(0))));
        doAnswer(invocation -> sectorMemory.put(invocation.getArgument(0), invocation.getArgument(1))).when(memory).set(anyString(), any());
        doAnswer(invocation -> sectorMemory.remove((String) invocation.getArgument(0))).when(memory).unset(anyString());
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(memory);
    }

    @AfterEach
    void tearDown() {
        mockSector.close();
        MasterSchematic.requiredWhen(null);
        HullUpgradesSetting.setMasterSchematicsRequiredForTests(HullUpgradesSetting.DEFAULT_MASTER_SCHEMATICS_REQUIRED);
        HullUpgradesSetting.setEnabledForTests(HullUpgradesSetting.DEFAULT_ENABLED);
    }

    private static ShipSkillData shipWithNodes(int nodeCount) {
        ShipSkillData shipData = new ShipSkillData();
        for (int node = 0; node < nodeCount; node++) {
            shipData.getAllocatedNodeIds().add("node_" + node);
        }
        return shipData;
    }

    @Test
    void masterSchematicsAreOnlyRequiredWhileHullUpgradesAreOn() {
        assertTrue(MasterSchematic.required());

        HullUpgradesSetting.setEnabledForTests(false);

        assertFalse(MasterSchematic.required());
        assertTrue(MasterSchematic.treeOpen(new ShipSkillData()));
    }

    @Test
    void aMasterSchematicOpensTheShipAndIsUsedUp() {
        ShipSkillData shipData = new ShipSkillData();
        store.addMasterSchematics(2);

        assertNull(HullUpgradeRules.masterSchematicBlock(shipData, store.masterSchematicCount()));
        assertTrue(HullUpgradeRules.applyMasterSchematic(shipData, store));

        assertTrue(shipData.isMasterSchematicd());
        assertEquals(1, store.masterSchematicCount());
        assertEquals(HullUpgradeRules.MasterSchematicBlock.ALREADY_UNLOCKED, HullUpgradeRules.masterSchematicBlock(shipData, 1));
        assertFalse(HullUpgradeRules.applyMasterSchematic(shipData, store));
        assertEquals(1, store.masterSchematicCount());
    }

    @Test
    void aMasterSchematicIsRefusedWithoutOneInStorageOrWhileNotRequired() {
        ShipSkillData shipData = new ShipSkillData();

        assertEquals(HullUpgradeRules.MasterSchematicBlock.NONE_IN_STORAGE, HullUpgradeRules.masterSchematicBlock(shipData, 0));
        assertFalse(HullUpgradeRules.applyMasterSchematic(shipData, store));

        HullUpgradesSetting.setMasterSchematicsRequiredForTests(false);
        store.addMasterSchematics(1);
        assertEquals(HullUpgradeRules.MasterSchematicBlock.NOT_REQUIRED, HullUpgradeRules.masterSchematicBlock(shipData, 1));
        assertFalse(HullUpgradeRules.applyMasterSchematic(shipData, store));
        assertEquals(1, store.masterSchematicCount());
        assertFalse(shipData.isMasterSchematicd());
    }

    @Test
    void switchingOnUnlocksEveryShipThatAlreadyHasNodesBesidesItsRoot() {
        ShipSkillData rootOnly = shipWithNodes(1);
        ShipSkillData built = shipWithNodes(3);
        ShipSkillData npcBuild = shipWithNodes(3);
        npcBuild.markNpcBuild();

        assertEquals(1, MasterSchematicActivation.unlockShipsWithNodes(List.of(rootOnly, built, npcBuild)));

        assertFalse(rootOnly.isMasterSchematicd());
        assertTrue(built.isMasterSchematicd());
        assertFalse(npcBuild.isMasterSchematicd());
    }

    @Test
    void aSaveWithNoUnlockedShipGetsOneStarterMasterSchematicOnce() {
        ShipSkillDataManager.put("fresh", shipWithNodes(1));

        MasterSchematicActivation.sync(mockSector.sector);
        assertEquals(1, ItemStore.get().masterSchematicCount());

        HullUpgradesSetting.setMasterSchematicsRequiredForTests(false);
        MasterSchematicActivation.sync(mockSector.sector);
        HullUpgradesSetting.setMasterSchematicsRequiredForTests(true);
        MasterSchematicActivation.sync(mockSector.sector);

        assertEquals(1, ItemStore.get().masterSchematicCount());
    }

    @Test
    void aSaveWhoseShipsAlreadyHaveNodesGetsNoStarterMasterSchematic() {
        ShipSkillData built = shipWithNodes(4);
        ShipSkillDataManager.put("veteran", built);

        MasterSchematicActivation.sync(mockSector.sector);

        assertTrue(built.isMasterSchematicd());
        assertEquals(0, ItemStore.get().masterSchematicCount());
    }

    @Test
    void aShipCapturedWhileMasterSchematicsAreOnIsNotUnlockedByALaterLoad() {
        MasterSchematicActivation.sync(mockSector.sector);
        ShipSkillData captured = shipWithNodes(5);
        ShipSkillDataManager.put("captured", captured);

        MasterSchematicActivation.sync(mockSector.sector);

        assertFalse(captured.isMasterSchematicd());
    }
}
