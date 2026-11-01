package exiledsector.items;

import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.CargoStackAPI;
import com.fs.starfarer.api.campaign.SpecialItemData;
import exiledsector.skills.ShipSkillData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MasterSchematicTest {

    @AfterEach
    void tearDown() {
        MasterSchematic.requiredWhen(null);
    }

    private static CargoStackAPI stack(SpecialItemData special, float size) {
        CargoStackAPI stack = mock(CargoStackAPI.class);
        when(stack.getSpecialDataIfSpecial()).thenReturn(special);
        when(stack.getSize()).thenReturn(size);
        return stack;
    }

    @Test
    void everyTreeIsOpenWhileMasterSchematicsAreNotRequired() {
        assertTrue(MasterSchematic.treeOpen(new ShipSkillData()));
        assertTrue(MasterSchematic.treeOpen(null));
    }

    @Test
    void whileMasterSchematicsAreRequiredOnlyUnlockedShipsAndNpcBuildsAreOpen() {
        MasterSchematic.requiredWhen(() -> true);
        ShipSkillData unlocked = new ShipSkillData();
        unlocked.unlockSkillTree();
        ShipSkillData npcBuild = new ShipSkillData();
        npcBuild.markNpcBuild();

        assertFalse(MasterSchematic.treeOpen(new ShipSkillData()));
        assertFalse(MasterSchematic.treeOpen(null));
        assertTrue(MasterSchematic.treeOpen(unlocked));
        assertTrue(MasterSchematic.treeOpen(npcBuild));
    }

    @Test
    void anUnlockedTreeIsProgressSoTheShipsRecordIsNeverDroppedAsBlank() {
        ShipSkillData shipData = new ShipSkillData();
        int revision = shipData.revision();

        assertTrue(shipData.unlockSkillTree());

        assertTrue(shipData.isMasterSchematicd());
        assertFalse(shipData.isBlank());
        assertTrue(shipData.revision() != revision);
        assertFalse(shipData.unlockSkillTree());
    }

    @Test
    void storageCountsMasterSchematicsAndTakesThemOneAtATime() {
        ItemStore store = new ItemStore();
        int revision = store.shipCurrencyRevision();

        assertFalse(store.takeMasterSchematic());
        store.addMasterSchematics(2);
        assertEquals(2, store.masterSchematicCount());
        assertTrue(store.shipCurrencyRevision() != revision);
        assertTrue(store.takeMasterSchematic());
        assertTrue(store.takeMasterSchematic());
        assertFalse(store.takeMasterSchematic());
        assertEquals(0, store.masterSchematicCount());
    }

    @Test
    void cargoStacksOfMasterSchematicsMoveIntoStorage() {
        ItemStore store = new ItemStore();
        CargoAPI cargo = mock(CargoAPI.class);
        CargoStackAPI schematics = stack(MasterSchematic.INSTANCE.toSpecialItem(), 3f);

        assertEquals(3, MasterSchematic.STACK_ABSORBER.absorb(store, cargo, schematics));
        assertEquals(3, store.masterSchematicCount());
        verify(cargo).removeStack(schematics);
        assertNull(MasterSchematic.STACK_ABSORBER.absorb(store, cargo, stack(RefitSchematic.STANDARD.toSpecialItem(), 1f)));
    }

    @Test
    void salvageOnlyRollsMasterSchematicsWhileTheyAreRequired() {
        assertEquals(List.of(), MasterSchematic.rollSalvage(10f, new Random(1L)));

        MasterSchematic.requiredWhen(() -> true);
        assertEquals(List.of(MasterSchematic.INSTANCE), MasterSchematic.rollSalvage(10f, new Random(1L)));
        assertEquals(List.of(), MasterSchematic.rollSalvage(0f, new Random(1L)));
    }

    @Test
    void listenersHearEveryMasterSchematicChange() {
        AtomicInteger heard = new AtomicInteger();
        Runnable listener = heard::incrementAndGet;
        MasterSchematic.onChange(listener);
        MasterSchematic.onChange(listener);

        MasterSchematic.changed();

        assertEquals(1, heard.get());
    }
}
