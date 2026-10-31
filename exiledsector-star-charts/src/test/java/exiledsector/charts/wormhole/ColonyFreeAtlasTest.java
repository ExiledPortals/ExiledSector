package exiledsector.charts.wormhole;

import com.fs.starfarer.api.campaign.CharacterDataAPI;
import com.fs.starfarer.api.campaign.PersistentUIDataAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import exiledsector.MockSector;
import exiledsector.charts.MapMemory;
import exiledsector.charts.StarChartSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ColonyFreeAtlasTest {

    private static final int SLOTS_PER_BAR = 10;

    private MockSector mockSector;
    private final Set<String> abilities = new HashSet<>();
    private final FakeSlots slots = new FakeSlots();
    private final MemoryAPI sectorMemory = MapMemory.create();

    @BeforeEach
    void setUp() {
        mockSector = new MockSector();
        mockSector.playerFleet();
        CharacterDataAPI character = mock(CharacterDataAPI.class);
        when(character.getAbilities()).thenReturn(abilities);
        doAnswer(invocation -> abilities.add(invocation.getArgument(0))).when(character).addAbility(anyString());
        doAnswer(invocation -> abilities.remove(invocation.getArgument(0))).when(character).removeAbility(anyString());
        when(mockSector.sector.getCharacterData()).thenReturn(character);
        PersistentUIDataAPI uiData = mock(PersistentUIDataAPI.class);
        when(uiData.getAbilitySlotsAPI()).thenReturn(slots);
        when(mockSector.sector.getUIData()).thenReturn(uiData);
        when(mockSector.sector.getMemoryWithoutUpdate()).thenReturn(sectorMemory);
        StarChartSettings.setNoColonyForTests(true);
    }

    @AfterEach
    void tearDown() {
        StarChartSettings.setNoColonyForTests(StarChartSettings.DEFAULT_NO_COLONY);
        mockSector.close();
    }

    @Test
    void theAbilityWaitsForTheBlueprint() {
        ColonyFreeAtlas.syncAbility(mockSector.sector);

        assertFalse(abilities.contains(ColonyFreeAtlas.ABILITY_ID));
    }

    @Test
    void theBlueprintGrantsTheAbilityIntoTheFirstFreeSlotOnce() {
        slots.bars.get(0).get(0).setAbilityId("transponder");
        slots.setCurrBarIndex(2);
        WormholeGateway.unlock(mockSector.sector);

        ColonyFreeAtlas.syncAbility(mockSector.sector);
        ColonyFreeAtlas.syncAbility(mockSector.sector);

        assertTrue(abilities.contains(ColonyFreeAtlas.ABILITY_ID));
        assertEquals(ColonyFreeAtlas.ABILITY_ID, slots.bars.get(0).get(1).getAbilityId());
        assertEquals(1, slots.bars.stream().flatMap(List::stream).filter(slot -> ColonyFreeAtlas.ABILITY_ID.equals(slot.getAbilityId())).count());
        assertEquals(2, slots.getCurrBarIndex());
    }

    @Test
    void switchingTheSettingOffTakesTheAbilityAndItsSlotsAway() {
        WormholeGateway.unlock(mockSector.sector);
        ColonyFreeAtlas.syncAbility(mockSector.sector);
        slots.bars.get(3).get(4).setInHyperAbilityId(ColonyFreeAtlas.ABILITY_ID);
        StarChartSettings.setNoColonyForTests(false);

        ColonyFreeAtlas.syncAbility(mockSector.sector);

        assertFalse(abilities.contains(ColonyFreeAtlas.ABILITY_ID));
        assertNull(slots.bars.get(0).get(0).getAbilityId());
        assertNull(slots.bars.get(3).get(4).getInHyperAbilityId());
    }

    @Test
    void starChartsSwitchedOffAlsoTakeTheAbilityAway() {
        WormholeGateway.unlock(mockSector.sector);
        ColonyFreeAtlas.syncAbility(mockSector.sector);
        StarChartSettings.setEnabledForTests(false);
        try {
            ColonyFreeAtlas.syncAbility(mockSector.sector);

            assertFalse(abilities.contains(ColonyFreeAtlas.ABILITY_ID));
        } finally {
            StarChartSettings.setEnabledForTests(StarChartSettings.DEFAULT_ENABLED);
        }
    }

    private static final class FakeSlots implements PersistentUIDataAPI.AbilitySlotsAPI {

        private final List<List<FakeSlot>> bars = new ArrayList<>();
        private int currentBar;

        private FakeSlots() {
            for (int bar = 0; bar < ColonyFreeAtlas.ABILITY_BARS; bar++) {
                List<FakeSlot> barSlots = new ArrayList<>();
                for (int slot = 0; slot < SLOTS_PER_BAR; slot++) {
                    barSlots.add(new FakeSlot(slot));
                }
                bars.add(barSlots);
            }
        }

        @Override
        public List<PersistentUIDataAPI.AbilitySlotAPI> getCurrSlotsCopy() {
            return new ArrayList<>(bars.get(currentBar));
        }

        @Override
        public int getCurrBarIndex() {
            return currentBar;
        }

        @Override
        public void setCurrBarIndex(int index) {
            currentBar = index;
        }

        @Override
        public boolean isLocked() {
            return false;
        }

        @Override
        public void setLocked(boolean locked) {
        }
    }

    private static final class FakeSlot implements PersistentUIDataAPI.AbilitySlotAPI {

        private final int slotId;
        private String abilityId;
        private String inHyperAbilityId;

        private FakeSlot(int slotId) {
            this.slotId = slotId;
        }

        @Override
        public int getSlotId() {
            return slotId;
        }

        @Override
        public String getAbilityId() {
            return abilityId;
        }

        @Override
        public void setAbilityId(String abilityId) {
            this.abilityId = abilityId;
        }

        @Override
        public String getInHyperAbilityId() {
            return inHyperAbilityId;
        }

        @Override
        public void setInHyperAbilityId(String inHyperAbilityId) {
            this.inHyperAbilityId = inHyperAbilityId;
        }
    }
}
