package exiledlib.skills.skilleffect.shield;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledlib.skills.skilleffect.CombatQueries;
import exiledlib.skills.skilleffect.SkillEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.AdditionalMatchers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SharedShieldDamageListenerTest {

    private static final String SHARED_PERCENT_KEY = "exiledLib_shieldDamageSharedPercent";
    private static final String PASSED_PERCENT_KEY = "exiledLib_shieldDamagePassedPercent";

    private final List<ShipAPI> ships = new ArrayList<>();

    private MockedStatic<Global> globalMock;

    private CombatEngineAPI engine;

    private CollisionGridAPI shipGrid;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        when(engine.getShips()).thenReturn(ships);
        shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<Object>(ships).iterator());
        when(engine.getTotalElapsedTime(false)).thenReturn(5f);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(String statKey, float statValue) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        when(dynamic.getValue(statKey, 0f)).thenReturn(statValue);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getLocation()).thenReturn(new Vector2f(0f, 0f));
        when(ship.getFluxTracker()).thenReturn(mock(FluxTrackerAPI.class));
        return ship;
    }

    private static ShipAPI shipAt(float x, int owner) {
        ShipAPI ship = ship(SHARED_PERCENT_KEY, 0f);
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getOwner()).thenReturn(owner);
        return ship;
    }

    private static <T> T attachedListener(SkillEffect effect, ShipAPI ship, Class<T> type) {
        effect.applyAfterShipCreation(ship, "mod_id", 0f);
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(ship).addListener(captor.capture());
        return type.cast(captor.getValue());
    }

    private static ApplyDamageResultAPI shieldFlux(float flux) {
        ApplyDamageResultAPI result = mock(ApplyDamageResultAPI.class);
        when(result.getDamageToShields()).thenReturn(flux);
        return result;
    }

    private static DamageAPI damageWithModifier(MutableStat modifier) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(modifier);
        return damage;
    }

    @Test
    void sharedFateMovesTheStatedShareSplitEvenlyAcrossNearbyAlliedShipsOnly() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ShipAPI allyA = shipAt(300f, 0);
        ShipAPI allyB = shipAt(-500f, 0);
        ShipAPI fighter = shipAt(100f, 0);
        when(fighter.isFighter()).thenReturn(true);
        ShipAPI farAlly = shipAt(1500f, 0);
        ShipAPI enemy = shipAt(200f, 1);
        ships.addAll(List.of(defender, allyA, allyB, fighter, farAlly, enemy));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);
        MutableStat modifier = new MutableStat(1f);
        Object projectile = new Object();

        when(defender.getParamAboutToApplyDamage()).thenReturn(projectile);

        String modifierId = listener.modifyDamageTaken(projectile, defender, damageWithModifier(modifier), new Vector2f(), true);
        ((DamageListener) listener).reportDamageApplied(enemy, defender, shieldFlux(80f));

        assertEquals(0.8f, modifier.getModifiedValue(), 1e-6f);
        modifier.unmodify(modifierId);
        assertEquals(1f, modifier.getModifiedValue(), 1e-6f);
        verify(allyA.getFluxTracker()).increaseFlux(10f, true);
        verify(allyB.getFluxTracker()).increaseFlux(10f, true);
        verifyNoInteractions(fighter.getFluxTracker(), farAlly.getFluxTracker(), enemy.getFluxTracker());
    }

    @Test
    void theTooltipShowsTheCappedShare() {
        assertEquals(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT.description(SharedShieldDamageListener.MAX_SHARED_PERCENT).plain(),
                ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT.description(120f).plain());
    }

    @Test
    void sharedFateNeverSendsFluxIntoItsOwnModulesOrItsParentsOtherModules() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ShipAPI ownModule = shipAt(100f, 0);
        when(ownModule.getParentStation()).thenReturn(defender);
        ShipAPI ally = shipAt(300f, 0);
        ships.addAll(List.of(defender, ownModule, ally));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);
        Object projectile = new Object();
        when(defender.getParamAboutToApplyDamage()).thenReturn(projectile);

        listener.modifyDamageTaken(projectile, defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        ((DamageListener) listener).reportDamageApplied(projectile, defender, shieldFlux(80f));

        verify(ally.getFluxTracker()).increaseFlux(20f, true);
        verify(ownModule.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());

        ShipAPI parent = shipAt(-100f, 0);
        ShipAPI sibling = shipAt(200f, 0);
        when(defender.getParentStation()).thenReturn(parent);
        when(sibling.getParentStation()).thenReturn(parent);
        assertTrue(CombatQueries.isSameStation(defender, parent));
        assertTrue(CombatQueries.isSameStation(defender, sibling));
        assertFalse(CombatQueries.isSameStation(defender, ally));
    }

    @Test
    void sharedFateSkipsAlliesThatAreOverloadedOrVenting() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ShipAPI ready = shipAt(300f, 0);
        ShipAPI busy = shipAt(-300f, 0);
        when(busy.getFluxTracker().isOverloadedOrVenting()).thenReturn(true);
        ships.addAll(List.of(defender, ready, busy));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);
        Object projectile = new Object();
        when(defender.getParamAboutToApplyDamage()).thenReturn(projectile);

        listener.modifyDamageTaken(projectile, defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        ((DamageListener) listener).reportDamageApplied(projectile, defender, shieldFlux(80f));

        verify(ready.getFluxTracker()).increaseFlux(20f, true);
        verify(busy.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
    }

    @Test
    void sharedFateLeavesTheHitAloneWithNoAlliesNearbyOrOnHullHits() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ships.add(defender);
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);
        DamageAPI damage = mock(DamageAPI.class);

        assertNull(listener.modifyDamageTaken(new Object(), defender, damage, new Vector2f(), true));
        ships.add(shipAt(300f, 0));
        assertNull(listener.modifyDamageTaken(new Object(), defender, damage, new Vector2f(), false));

        verifyNoInteractions(damage);
    }

    @Test
    void sharedFateOnlySharesTheHitItReduced() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ShipAPI ally = shipAt(300f, 0);
        ships.addAll(List.of(defender, ally));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);

        Object reducedHit = new Object();
        when(defender.getParamAboutToApplyDamage()).thenReturn(reducedHit);
        listener.modifyDamageTaken(reducedHit, defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        when(defender.getParamAboutToApplyDamage()).thenReturn(new Object());
        ((DamageListener) listener).reportDamageApplied(new Object(), defender, shieldFlux(80f));

        verify(ally.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
    }

    @Test
    void sharedFateLooksForAlliesOnlyEveryQuarterSecondWhileUnderSustainedFire() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ships.addAll(List.of(defender, shipAt(300f, 0)));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);

        listener.modifyDamageTaken(new Object(), defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        listener.modifyDamageTaken(new Object(), defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        when(engine.getTotalElapsedTime(false)).thenReturn(5.1f);
        listener.modifyDamageTaken(new Object(), defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        verify(shipGrid, times(1)).getCheckIterator(any(), anyFloat(), anyFloat());

        when(engine.getTotalElapsedTime(false)).thenReturn(5.3f);
        listener.modifyDamageTaken(new Object(), defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        verify(shipGrid, times(2)).getCheckIterator(any(), anyFloat(), anyFloat());
    }

    @Test
    void sharedFateStillSkipsAnAllyThatStartedVentingSinceTheLastSearch() {
        ShipAPI defender = ship(SHARED_PERCENT_KEY, 20f);
        ShipAPI ally = shipAt(300f, 0);
        ShipAPI soonVenting = shipAt(-300f, 0);
        ships.addAll(List.of(defender, ally, soonVenting));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_SHARED_PERCENT, defender, DamageTakenModifier.class);
        listener.modifyDamageTaken(new Object(), defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);

        when(soonVenting.getFluxTracker().isOverloadedOrVenting()).thenReturn(true);
        when(engine.getTotalElapsedTime(false)).thenReturn(5.1f);
        Object projectile = new Object();
        when(defender.getParamAboutToApplyDamage()).thenReturn(projectile);
        listener.modifyDamageTaken(projectile, defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        ((DamageListener) listener).reportDamageApplied(projectile, defender, shieldFlux(80f));

        verify(shipGrid, times(1)).getCheckIterator(any(), anyFloat(), anyFloat());
        verify(ally.getFluxTracker()).increaseFlux(20f, true);
        verify(soonVenting.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
    }

    private static ShipAPI allyWithFluxRoom(float x, float room) {
        ShipAPI ally = shipAt(x, 0);
        when(ally.getFluxTracker().getMaxFlux()).thenReturn(1000f);
        when(ally.getFluxTracker().getCurrFlux()).thenReturn(1000f - room);
        return ally;
    }

    @Test
    void passedShieldDamageGoesToTheNearestAllyWithFluxRoomAsSoftFlux() {
        ShipAPI defender = ship(PASSED_PERCENT_KEY, 30f);
        ShipAPI fullAlly = allyWithFluxRoom(100f, 0f);
        ShipAPI nearAlly = allyWithFluxRoom(300f, 500f);
        ShipAPI farAlly = allyWithFluxRoom(-600f, 500f);
        ships.addAll(List.of(defender, fullAlly, nearAlly, farAlly));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_PASSED_TO_ALLY_PERCENT, defender, DamageTakenModifier.class);
        MutableStat modifier = new MutableStat(1f);
        Object projectile = new Object();
        when(defender.getParamAboutToApplyDamage()).thenReturn(projectile);

        listener.modifyDamageTaken(projectile, defender, damageWithModifier(modifier), new Vector2f(), true);
        ((DamageListener) listener).reportDamageApplied(projectile, defender, shieldFlux(70f));

        assertEquals(0.7f, modifier.getModifiedValue(), 1e-6f);
        verify(nearAlly.getFluxTracker()).increaseFlux(AdditionalMatchers.eq(30f, 1e-3f), eq(false));
        verify(fullAlly.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
        verify(farAlly.getFluxTracker(), never()).increaseFlux(anyFloat(), anyBoolean());
    }

    @Test
    void passedShieldDamageTheAllyHasNoRoomForStaysWithTheShipAsHardFlux() {
        ShipAPI defender = ship(PASSED_PERCENT_KEY, 30f);
        ShipAPI ally = allyWithFluxRoom(300f, 500f);
        ships.addAll(List.of(defender, ally));
        DamageTakenModifier listener = attachedListener(ShieldSkillEffect.SHIELD_DAMAGE_PASSED_TO_ALLY_PERCENT, defender, DamageTakenModifier.class);
        Object projectile = new Object();
        when(defender.getParamAboutToApplyDamage()).thenReturn(projectile);

        listener.modifyDamageTaken(projectile, defender, damageWithModifier(new MutableStat(1f)), new Vector2f(), true);
        when(ally.getFluxTracker().getCurrFlux()).thenReturn(990f);
        ((DamageListener) listener).reportDamageApplied(projectile, defender, shieldFlux(70f));

        verify(ally.getFluxTracker()).increaseFlux(AdditionalMatchers.eq(10f, 1e-3f), eq(false));
        verify(defender.getFluxTracker()).increaseFlux(AdditionalMatchers.eq(20f, 1e-3f), eq(true));
    }
}
