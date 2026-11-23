package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ActiveVentingDisabledTest {

    @Test
    void ventingSpeedDropsToZeroWhateverOtherSourcesAdd() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat ventRate = new MutableStat(1f);
        when(stats.getVentRateMult()).thenReturn(ventRate);

        FluxSkillEffect.VENT_RATE_MULT.apply(stats, "vents", 50f);
        FluxSkillEffect.ACTIVE_VENTING_DISABLED.apply(stats, "overrides", 0f);

        assertEquals(0f, ventRate.getModifiedValue(), 1e-6f);
    }

    @Test
    void theLineReadsAsVanillaWordsIt() {
        assertEquals("Prevents the use of active venting.", FluxSkillEffect.ACTIVE_VENTING_DISABLED.description(0f).plain());
    }
}
