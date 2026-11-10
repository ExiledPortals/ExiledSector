package exiledsector.skills.skilleffect.onhit;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.ShipAPI;
import exiledsector.skills.skilleffect.EmpOnlyHits;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IonSpillListenerTest {

    private MockedStatic<Global> globalMock;
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(int owner) {
        ShipAPI ship = mock(ShipAPI.class, Mockito.RETURNS_DEEP_STUBS);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getParentStation()).thenReturn(null);
        when(ship.getShield().isWithinArc(any())).thenReturn(true);
        return ship;
    }

    private static ShipAPI owner(float sharePercent) {
        ShipAPI owner = ship(0);
        when(owner.getMutableStats().getDynamic().getValue(IonSpillListener.ENERGY_SHARE_PERCENT_KEY, 0f)).thenReturn(sharePercent);
        return owner;
    }

    private static DamageAPI empHit(float emp) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.computeFluxDealt(anyFloat())).thenReturn(emp);
        return damage;
    }

    private void verifyNoDamageDealt() {
        verify(engine, never()).applyDamage(any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any());
    }

    @Test
    void theShareOfEmpOnAShieldLandsThereAsEnergyDamageOnTheNextFrame() {
        ShipAPI owner = owner(25f);
        ShipAPI target = ship(1);
        IonSpillListener listener = new IonSpillListener(owner);
        Vector2f hitPoint = new Vector2f(30f, 40f);

        listener.modifyDamageDealt(null, target, empHit(200f), hitPoint, true);
        verifyNoDamageDealt();

        listener.advance(0.016f);
        verify(engine).applyDamage(eq(target), eq(hitPoint), eq(50f), eq(DamageType.ENERGY), eq(0f), eq(false), eq(false), eq(owner));
    }

    @Test
    void hullHitsAlliesAndHitsWithoutEmpSpillNothing() {
        IonSpillListener listener = new IonSpillListener(owner(25f));

        listener.modifyDamageDealt(null, ship(1), empHit(200f), new Vector2f(), false);
        listener.modifyDamageDealt(null, ship(0), empHit(200f), new Vector2f(), true);
        listener.modifyDamageDealt(null, ship(1), empHit(0f), new Vector2f(), true);
        listener.advance(0.016f);

        verifyNoDamageDealt();
    }

    @Test
    void theHullSizeDamageBonusIsLeftToTheEngineOnTheSpilledHit() {
        ShipAPI owner = owner(25f);
        when(owner.getMutableStats().getDamageToCruisers().getModifiedValue()).thenReturn(1.5f);
        ShipAPI cruiser = ship(1);
        when(cruiser.isCruiser()).thenReturn(true);
        IonSpillListener listener = new IonSpillListener(owner);

        listener.modifyDamageDealt(null, cruiser, empHit(200f), new Vector2f(), true);
        listener.advance(0.016f);

        verify(engine).applyDamage(eq(cruiser), any(), eq(50f), eq(DamageType.ENERGY), eq(0f), eq(false), eq(false), eq(owner));
    }

    @Test
    void anEmpOnlyShieldHitSpillsItsEmpIncludingTheBonus() {
        ShipAPI owner = owner(25f);
        ShipAPI target = ship(1);
        IonSpillListener listener = new IonSpillListener(owner);

        listener.onEmpOnlyHit(new EmpOnlyHits.EmpOnlyHit(target, new Vector2f(), 100f, 200f, true));
        listener.onEmpOnlyHit(new EmpOnlyHits.EmpOnlyHit(target, new Vector2f(), 100f, 200f, false));
        listener.advance(0.016f);

        verify(engine).applyDamage(eq(target), any(), eq(50f), eq(DamageType.ENERGY), eq(0f), eq(false), eq(false), eq(owner));
    }

    @Test
    void aShieldLoweredOrTurnedAwayBeforeTheNextFrameTakesNoSpill() {
        ShipAPI target = ship(1);
        IonSpillListener listener = new IonSpillListener(owner(25f));

        listener.modifyDamageDealt(null, target, empHit(200f), new Vector2f(), true);
        when(target.getShield().isWithinArc(any())).thenReturn(false);
        listener.advance(0.016f);

        verifyNoDamageDealt();
    }

    @Test
    void aTargetDestroyedBeforeTheNextFrameTakesNoSpill() {
        ShipAPI target = ship(1);
        IonSpillListener listener = new IonSpillListener(owner(25f));

        listener.modifyDamageDealt(null, target, empHit(200f), new Vector2f(), true);
        when(target.isAlive()).thenReturn(false);
        listener.advance(0.016f);

        verifyNoDamageDealt();
    }
}
