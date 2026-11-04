package exiledsector.skills.skilleffect.flux;

import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HardFluxFreeDissipationTest {

    private static final float EPSILON = 1e-4f;

    private static ShipAPI ship(float hardFlux, MutableStat fluxDissipation) {
        ShipAPI ship = mock(ShipAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getFluxTracker().getHardFlux()).thenReturn(hardFlux);
        when(ship.getMutableStats().getFluxDissipation()).thenReturn(fluxDissipation);
        return ship;
    }

    @Test
    void dissipationRisesOnlyWhileTheShipHasNoHardFlux() {
        MutableStat fluxDissipation = new MutableStat(100f);
        FluxSkillEffect effect = FluxSkillEffect.FLUX_DISSIPATION_WITHOUT_HARD_FLUX_PERCENT;

        effect.advanceInCombat(ship(0f, fluxDissipation), "mod_id", 15f);
        assertEquals(115f, fluxDissipation.getModifiedValue(), EPSILON);

        effect.advanceInCombat(ship(40f, fluxDissipation), "mod_id", 15f);
        assertEquals(100f, fluxDissipation.getModifiedValue(), EPSILON);
    }
}
