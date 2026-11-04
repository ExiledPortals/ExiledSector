package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.StatBonus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OverloadDurationTest {

    @Test
    void overloadDurationIsSummedWithTheOverloadTimeModsOtherPercentages() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        StatBonus overloadTime = mock(StatBonus.class);
        when(stats.getOverloadTimeMod()).thenReturn(overloadTime);

        FluxSkillEffect.OVERLOAD_DURATION_PERCENT.apply(stats, "degenerate", -15f);

        verify(overloadTime).modifyPercent("degenerate", -15f);
        assertTrue(FluxSkillEffect.OVERLOAD_DURATION_PERCENT.lowerIsBetter());
    }
}
