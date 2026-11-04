package exiledsector.skills.skilleffect.special;

import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BorrowedGraceListenerTest {

    private static final String GRACE_MOD_ID = "exiledLib_seraphsGrace_grace";

    private static ShipAPI ship(String id, int owner, Map<String, Float> magnitudes) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.getMutableStats()).thenReturn(stats);
        magnitudes.forEach((key, value) -> when(stats.getDynamic().getValue(key, 0f)).thenReturn(value));
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static ApplyDamageResultAPI hit(float armor, float hull) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getTotalDamageToArmor()).thenReturn(armor);
        when(result.getDamageToHull()).thenReturn(hull);
        return result;
    }

    private static ShipAPI graceShip(float maxStacks) {
        return ship("grace", 0, Map.of(BorrowedGraceListener.MAX_STACKS_KEY, maxStacks));
    }

    @Test
    void graceStacksBuildFromArmorAndHullDamageTakenAndKeepTheRemainder() {
        ShipAPI ship = graceShip(20f);
        BorrowedGraceListener grace = new BorrowedGraceListener(ship);
        MutableShipStatsAPI stats = ship.getMutableStats();

        grace.reportDamageApplied(null, mock(ShipAPI.class), hit(500f, 500f));
        assertEquals(0, grace.stacks());

        grace.reportDamageApplied(null, ship, hit(90f, 60f));
        assertEquals(1, grace.stacks());
        verify(stats.getMaxSpeed()).modifyPercent(GRACE_MOD_ID, 1f);
        verify(stats.getHullDamageTakenMult()).modifyPercent(GRACE_MOD_ID, -1f);
        verify(stats.getEnergyWeaponFluxCostMod()).modifyPercent(GRACE_MOD_ID, -1f);

        grace.reportDamageApplied(null, ship, hit(0f, 60f));
        assertEquals(2, grace.stacks());
        verify(stats.getFluxDissipation()).modifyPercent(GRACE_MOD_ID, 2f);
    }

    @Test
    void slowStacksAloneComeEvery150DamageWithTheFullStackEffect() {
        ShipAPI ship = ship("grace", 0, Map.of(BorrowedGraceListener.SLOW_MAX_STACKS_KEY, 10f));
        BorrowedGraceListener grace = new BorrowedGraceListener(ship);

        grace.reportDamageApplied(null, ship, hit(0f, 100f));
        assertEquals(0, grace.stacks());

        grace.reportDamageApplied(null, ship, hit(50f, 0f));
        assertEquals(1, grace.stacks());
        verify(ship.getMutableStats().getMaxSpeed()).modifyPercent(GRACE_MOD_ID, BorrowedGraceListener.PERCENT_PER_STACK);
    }

    @Test
    void theNodeAndSlowStacksShareOnePoolAtTheNodesFasterRate() {
        ShipAPI ship = ship("grace", 0, Map.of(BorrowedGraceListener.MAX_STACKS_KEY, 2f, BorrowedGraceListener.SLOW_MAX_STACKS_KEY, 1f));
        BorrowedGraceListener grace = new BorrowedGraceListener(ship);

        assertEquals(3, grace.maxStacks());
        assertEquals(BorrowedGraceListener.DAMAGE_PER_STACK, grace.damagePerStack());
        grace.reportDamageApplied(null, ship, hit(0f, 400f));
        assertEquals(3, grace.stacks());
    }

    @Test
    void graceStacksExpireAfterTheirDurationButNotWhilePhased() {
        ShipAPI ship = graceShip(20f);
        BorrowedGraceListener grace = new BorrowedGraceListener(ship);

        grace.reportDamageApplied(null, ship, hit(0f, 100f));
        when(ship.isPhased()).thenReturn(true);
        grace.advance(BorrowedGraceListener.STACK_DURATION_SECONDS * 2f);
        assertEquals(1, grace.stacks());

        when(ship.isPhased()).thenReturn(false);
        grace.advance(BorrowedGraceListener.STACK_DURATION_SECONDS);
        assertEquals(0, grace.stacks());
        verify(ship.getMutableStats().getMaxSpeed()).unmodify(GRACE_MOD_ID);
    }

    @Test
    void graceReplacesTheOldestStackAtTheCap() {
        ShipAPI ship = graceShip(2f);
        BorrowedGraceListener grace = new BorrowedGraceListener(ship);

        grace.reportDamageApplied(null, ship, hit(0f, 100f));
        grace.advance(10f);
        grace.reportDamageApplied(null, ship, hit(0f, 200f));
        assertEquals(2, grace.stacks());

        grace.advance(BorrowedGraceListener.STACK_DURATION_SECONDS - 1f);
        assertEquals(2, grace.stacks());
        grace.advance(1f);
        assertEquals(0, grace.stacks());
    }
}
