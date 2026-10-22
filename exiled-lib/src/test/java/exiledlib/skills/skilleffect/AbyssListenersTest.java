package exiledlib.skills.skilleffect;

import com.fs.starfarer.api.GameState;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.util.Misc;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AbyssListenersTest {

    private static final String GRACE_MOD_ID = "exiledSector_seraphsGrace_grace";
    private static final String SERAPHIM_MOD_ID = "exiledSector_seraphim_hunter";

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
        return ship("grace", 0, Map.of(SeraphsGraceListener.MAX_STACKS_KEY, maxStacks, SeraphsGraceListener.PERCENT_PER_STACK_KEY, 1f));
    }

    @Test
    void graceStacksBuildFromArmorAndHullDamageTakenAndKeepTheRemainder() {
        ShipAPI ship = graceShip(20f);
        SeraphsGraceListener grace = new SeraphsGraceListener(ship);
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
    void graceStacksExpireAfterTheirDurationButNotWhilePhased() {
        ShipAPI ship = graceShip(20f);
        SeraphsGraceListener grace = new SeraphsGraceListener(ship);

        grace.reportDamageApplied(null, ship, hit(0f, 100f));
        when(ship.isPhased()).thenReturn(true);
        grace.advance(SeraphsGraceListener.STACK_DURATION_SECONDS * 2f);
        assertEquals(1, grace.stacks());

        when(ship.isPhased()).thenReturn(false);
        grace.advance(SeraphsGraceListener.STACK_DURATION_SECONDS);
        assertEquals(0, grace.stacks());
        verify(ship.getMutableStats().getMaxSpeed()).unmodify(GRACE_MOD_ID);
    }

    @Test
    void graceReplacesTheOldestStackAtTheCap() {
        ShipAPI ship = graceShip(2f);
        SeraphsGraceListener grace = new SeraphsGraceListener(ship);

        grace.reportDamageApplied(null, ship, hit(0f, 100f));
        grace.advance(10f);
        grace.reportDamageApplied(null, ship, hit(0f, 200f));
        assertEquals(2, grace.stacks());

        grace.advance(SeraphsGraceListener.STACK_DURATION_SECONDS - 1f);
        assertEquals(2, grace.stacks());
        grace.advance(1f);
        assertEquals(0, grace.stacks());
    }

    private static ShipAPI seraphimShip(float hitpoints) {
        ShipAPI ship = ship("hunter", 0, Map.of(SeraphimListener.HEAL_PERCENT_KEY, 10f, SeraphimListener.HEAL_CAP_PERCENT_KEY, 20f,
                SeraphimListener.DAMAGE_BONUS_PERCENT_KEY, 30f, SeraphimListener.FULL_BONUS_DAMAGE_KEY, 1000f));
        float[] currentHitpoints = {hitpoints};
        when(ship.getMaxHitpoints()).thenReturn(1000f);
        when(ship.getHitpoints()).thenAnswer(call -> currentHitpoints[0]);
        Mockito.doAnswer(call -> currentHitpoints[0] = call.getArgument(0)).when(ship).setHitpoints(anyFloat());
        return ship;
    }

    @Test
    void seraphimRepairsAShareOfDamageDealtUpToItsCap() {
        ShipAPI ship = seraphimShip(500f);
        SeraphimListener seraphim = new SeraphimListener(ship);
        ShipAPI enemy = ship("enemy", 1, Map.of());

        seraphim.onDamageDealt(enemy, hit(400f, 600f));
        assertEquals(600f, ship.getHitpoints(), 0.001f);

        seraphim.onDamageDealt(enemy, hit(0f, 5000f));
        assertEquals(700f, ship.getHitpoints(), 0.001f);
        assertEquals(200f, seraphim.hullRepaired(), 0.001f);

        seraphim.onDamageDealt(enemy, hit(0f, 5000f));
        assertEquals(700f, ship.getHitpoints(), 0.001f);
    }

    @Test
    void seraphimDamageRampsToItsFullBonusAndThenUnlocksTheZeroFluxBoost() {
        ShipAPI ship = seraphimShip(1000f);
        SeraphimListener seraphim = new SeraphimListener(ship);
        ShipAPI enemy = ship("enemy", 1, Map.of());
        MutableShipStatsAPI stats = ship.getMutableStats();

        seraphim.onDamageDealt(enemy, hit(250f, 250f));
        verify(stats.getEnergyWeaponDamageMult()).modifyPercent(SERAPHIM_MOD_ID, 15f);
        verify(stats.getZeroFluxMinimumFluxLevel(), never()).modifyFlat(anyString(), anyFloat());

        seraphim.onDamageDealt(enemy, hit(0f, 900f));
        verify(stats.getBallisticWeaponDamageMult()).modifyPercent(SERAPHIM_MOD_ID, 30f);
        verify(stats.getZeroFluxMinimumFluxLevel()).modifyFlat(SERAPHIM_MOD_ID, 2f);
    }

    @Test
    void seraphimIgnoresDamageToFriendlyShips() {
        ShipAPI ship = seraphimShip(500f);
        SeraphimListener seraphim = new SeraphimListener(ship);

        seraphim.onDamageDealt(ship("ally", 0, Map.of()), hit(0f, 1000f));

        assertEquals(0f, seraphim.damageDealt(), 0.001f);
        assertEquals(500f, ship.getHitpoints(), 0.001f);
    }

    @Test
    void seraphimIgnoresHulksAndNeutralWrecks() {
        ShipAPI ship = seraphimShip(500f);
        SeraphimListener seraphim = new SeraphimListener(ship);
        ShipAPI dyingEnemy = ship("dying", 1, Map.of());
        when(dyingEnemy.isHulk()).thenReturn(true);

        seraphim.onDamageDealt(dyingEnemy, hit(0f, 2000f));
        seraphim.onDamageDealt(ship("wreck", Misc.OWNER_NEUTRAL, Map.of()), hit(0f, 2000f));

        assertEquals(0f, seraphim.damageDealt(), 0.001f);
        assertEquals(500f, ship.getHitpoints(), 0.001f);
    }

    private record CombatRouting(CombatListenerManagerAPI listenerManager, List<Object> engineListeners) {

        SeraphimListener.DealtDamageRouter router() {
            return (SeraphimListener.DealtDamageRouter) engineListeners.get(0);
        }
    }

    private static CombatRouting combatRouting(MockedStatic<Global> global, GameState state) {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CombatListenerManagerAPI listenerManager = mock(CombatListenerManagerAPI.class);
        List<Object> engineListeners = new ArrayList<>();
        when(engine.getListenerManager()).thenReturn(listenerManager);
        Mockito.doAnswer(call -> engineListeners.add(call.getArgument(0))).when(listenerManager).addListener(Mockito.any());
        when(listenerManager.getListeners(SeraphimListener.DealtDamageRouter.class))
                .thenAnswer(call -> engineListeners.stream().map(SeraphimListener.DealtDamageRouter.class::cast).toList());
        global.when(Global::getCombatEngine).thenReturn(engine);
        global.when(Global::getSettings).thenReturn(mock(SettingsAPI.class));
        global.when(Global::getCurrentState).thenReturn(state);
        return new CombatRouting(listenerManager, engineListeners);
    }

    @Test
    void theEngineWideRouterSendsEachHitToTheShipThatDealtItIncludingItsDrones() {
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            CombatRouting routing = combatRouting(global, GameState.COMBAT);
            ShipAPI hunter = seraphimShip(500f);
            SeraphimListener seraphim = new SeraphimListener(hunter);
            ShipAPI enemy = ship("enemy", 1, Map.of());
            ShipAPI drone = ship("drone", 0, Map.of());
            SeraphimListener.DealtDamageRouter.routeDroneDamage(hunter, drone);

            routing.router().reportDamageApplied(ship("bystander", 0, Map.of()), enemy, hit(0f, 1000f));
            assertEquals(0f, seraphim.damageDealt(), 0.001f);

            routing.router().reportDamageApplied(hunter, enemy, hit(0f, 1000f));
            routing.router().reportDamageApplied(drone, enemy, hit(0f, 500f));
            assertEquals(1500f, seraphim.damageDealt(), 0.001f);
        }
    }

    @Test
    void shipsBuiltOutsideCombatAreNotRegistered() {
        try (MockedStatic<Global> global = Mockito.mockStatic(Global.class)) {
            CombatRouting routing = combatRouting(global, GameState.CAMPAIGN);
            new SeraphimListener(seraphimShip(500f));

            verify(routing.listenerManager(), never()).addListener(Mockito.any());
        }
    }
}
