package exiledsector.effects;

import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.econ.impl.TechMining;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TechMiningItemListenerTest {

    private static final float DECAY = 0.95f;

    private static MemoryAPI memory(Integer months) {
        MemoryAPI memory = mock(MemoryAPI.class);
        when(memory.contains(TechMiningItemListener.MONTHS_KEY)).thenReturn(months != null);
        when(memory.getInt(TechMiningItemListener.MONTHS_KEY)).thenReturn(months == null ? 0 : months);
        return memory;
    }

    private static TechMining vanillaAt(float mult) {
        TechMining techMining = mock(TechMining.class);
        when(techMining.getTechMiningMult()).thenReturn(mult);
        return techMining;
    }

    @Test
    void aFreshColonyStartsWithTheFirstFindEvenThoughVanillaDecayedJustBeforeUs() {
        assertEquals(0, TechMiningItemListener.monthsMined(memory(null), vanillaAt(DECAY), DECAY));
        assertEquals(0, TechMiningItemListener.monthsMined(memory(null), vanillaAt(1f), DECAY));
    }

    @Test
    void theCountedMonthsWinOnceRecorded() {
        assertEquals(7, TechMiningItemListener.monthsMined(memory(7), vanillaAt(1f), DECAY));
    }

    @Test
    void aColonyThatWasMiningBeforeTheUpdateSkipsTheFirstFindAndPicksUpVanillasDecay() {
        float afterTenFindsIncludingThisMonth = (float) Math.pow(DECAY, 10);

        assertEquals(9, TechMiningItemListener.monthsMined(memory(null), vanillaAt(afterTenFindsIncludingThisMonth), DECAY));
        assertEquals(0, TechMiningItemListener.monthsMined(memory(null), mock(Industry.class), DECAY));
    }
}
