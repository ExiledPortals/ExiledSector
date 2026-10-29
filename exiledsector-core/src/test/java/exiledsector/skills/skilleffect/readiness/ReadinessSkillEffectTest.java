package exiledsector.skills.skilleffect.readiness;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReadinessSkillEffectTest {

    @Test
    void maxCombatReadinessAddsTheMagnitudeAsAFlatFraction() {
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        MutableStat maxCr = mock(MutableStat.class);
        when(stats.getMaxCombatReadiness()).thenReturn(maxCr);

        ReadinessSkillEffect.MAX_COMBAT_READINESS_PERCENT.apply(stats, "mod_id", 5f);

        verify(maxCr).modifyFlat("mod_id", 0.05f, "Ship skill tree");
        assertEquals("5% increased maximum combat readiness.",
                ReadinessSkillEffect.MAX_COMBAT_READINESS_PERCENT.description(5f).plain());
    }
}
