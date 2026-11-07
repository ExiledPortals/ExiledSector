package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PirateFrenzyTest {

    private static final String FRENZY_MOD_ID = "exiledSector_pirateFrenzy_raider";

    private static ShipAPI ship() {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn("raider");
        when(ship.getMutableStats()).thenReturn(stats);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    @Test
    void eachStackRaisesWeaponRateOfFireAndCutsWeaponFluxCost() {
        ShipAPI ship = ship();
        PirateFrenzy frenzy = new PirateFrenzy(ship);
        MutableShipStatsAPI stats = ship.getMutableStats();

        frenzy.gain();
        verify(stats.getBallisticRoFMult()).modifyPercent(FRENZY_MOD_ID, PirateFrenzy.RATE_OF_FIRE_PERCENT_PER_STACK);
        verify(stats.getEnergyWeaponFluxCostMod()).modifyPercent(FRENZY_MOD_ID, -PirateFrenzy.FLUX_COST_REDUCTION_PERCENT_PER_STACK);

        frenzy.gain();
        assertEquals(2, frenzy.stacks());
        verify(stats.getMissileRoFMult()).modifyPercent(FRENZY_MOD_ID, 2f * PirateFrenzy.RATE_OF_FIRE_PERCENT_PER_STACK);
        verify(stats.getBallisticWeaponFluxCostMod()).modifyPercent(FRENZY_MOD_ID, -2f * PirateFrenzy.FLUX_COST_REDUCTION_PERCENT_PER_STACK);
    }

    @Test
    void eachStackExpiresOnItsOwnTimer() {
        ShipAPI ship = ship();
        PirateFrenzy frenzy = new PirateFrenzy(ship);

        frenzy.gain();
        frenzy.advance(4f);
        frenzy.gain();
        frenzy.advance(PirateFrenzy.STACK_SECONDS - 4f);
        assertEquals(1, frenzy.stacks());

        frenzy.advance(4f);
        assertEquals(0, frenzy.stacks());
        verify(ship.getMutableStats().getEnergyRoFMult()).unmodify(FRENZY_MOD_ID);
    }

    @Test
    void aStackGainedAtTheCapReplacesTheOldest() {
        ShipAPI ship = ship();
        PirateFrenzy frenzy = new PirateFrenzy(ship);

        frenzy.gain();
        frenzy.advance(5f);
        for (int i = 0; i < PirateFrenzy.MAX_STACKS; i++) {
            frenzy.gain();
        }
        assertEquals(PirateFrenzy.MAX_STACKS, frenzy.stacks());

        frenzy.advance(PirateFrenzy.STACK_SECONDS - 1f);
        assertEquals(PirateFrenzy.MAX_STACKS, frenzy.stacks());
        frenzy.advance(1f);
        assertEquals(0, frenzy.stacks());
    }

    @Test
    void aDestroyedShipLosesEveryStackAndGainsNoMore() {
        ShipAPI ship = ship();
        PirateFrenzy frenzy = new PirateFrenzy(ship);
        frenzy.gain();

        when(ship.isAlive()).thenReturn(false);
        frenzy.advance(0.1f);
        frenzy.gain();

        assertEquals(0, frenzy.stacks());
    }
}
