package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicFakeBeamPlugin;
import org.magiclib.util.MagicFakeBeam;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeamSplitListenerTest {

    private final List<ShipAPI> ships = new ArrayList<>();
    private MockedStatic<Global> globalMock;
    private MockedStatic<MagicFakeBeam> fakeBeamMock;
    private MockedStatic<MagicFakeBeamPlugin> fakeBeamPluginMock;
    private CombatEngineAPI engine;

    @BeforeEach
    void setUp() {
        engine = mock(CombatEngineAPI.class);
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<Object>(ships).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
        fakeBeamMock = Mockito.mockStatic(MagicFakeBeam.class);
        fakeBeamPluginMock = Mockito.mockStatic(MagicFakeBeamPlugin.class);
    }

    @AfterEach
    void tearDown() {
        fakeBeamPluginMock.close();
        fakeBeamMock.close();
        globalMock.close();
    }

    private static ShipAPI shipAt(float x, int owner) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(mock(DynamicStatsAPI.class));
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    private static BeamAPI beamFrom(WeaponAPI weapon) {
        BeamAPI beam = mock(BeamAPI.class);
        when(beam.getWeapon()).thenReturn(weapon);
        return beam;
    }

    private static WeaponAPI simulatedWeapon() {
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getRange()).thenReturn(1000f);
        when(weapon.getDerivedStats()).thenReturn(mock(WeaponAPI.DerivedWeaponStatsAPI.class));
        return weapon;
    }

    private static DamageAPI tick(boolean forceHardFlux) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getDamage()).thenReturn(100f);
        when(damage.getType()).thenReturn(DamageType.ENERGY);
        when(damage.isForceHardFlux()).thenReturn(forceHardFlux);
        when(damage.getModifier()).thenReturn(new MutableStat(1f));
        return damage;
    }

    private FluxTrackerAPI splitTargetFlux(ShipAPI splitTarget, float before, float after, boolean[] flagDuringHit) {
        FluxTrackerAPI flux = mock(FluxTrackerAPI.class);
        when(splitTarget.getFluxTracker()).thenReturn(flux);
        float[] current = {before};
        when(flux.getCurrFlux()).thenAnswer(invocation -> current[0]);
        when(flux.getHardFlux()).thenReturn(20f);
        doAnswer(invocation -> {
            flagDuringHit[0] = BeamSplitListener.isApplyingSimulatedHit();
            current[0] = after;
            return null;
        }).when(engine).applyDamage(any(), any(), any(), anyFloat(), any(), anyFloat(), anyBoolean(), anyBoolean(), any(), anyBoolean());
        return flux;
    }

    @Test
    void aSimulatedSplitHitTurnsTheStatedShareOfTheShieldFluxItAddedIntoHardFluxAfterTheEngineFinishes() {
        ShipAPI firing = shipAt(0f, 0);
        when(firing.getMutableStats().getDynamic().getValue(BeamSplitListener.TARGETS_KEY, 0f)).thenReturn(1f);
        when(firing.getMutableStats().getDynamic().getValue(ShieldSkillEffect.BeamHardFluxListener.HARD_FLUX_PERCENT_KEY, 0f)).thenReturn(50f);
        ShipAPI primary = shipAt(100f, 1);
        ShipAPI splitTarget = shipAt(200f, 1);
        ships.addAll(List.of(firing, primary, splitTarget));
        boolean[] flagDuringHit = {false};
        FluxTrackerAPI flux = splitTargetFlux(splitTarget, 100f, 150f, flagDuringHit);

        new BeamSplitListener(firing).modifyDamageDealt(beamFrom(simulatedWeapon()), primary, tick(false), new Vector2f(100f, 0f), true);

        assertTrue(flagDuringHit[0]);
        assertFalse(BeamSplitListener.isApplyingSimulatedHit());
        verify(flux).setHardFlux(45f);
    }

    @Test
    void aSimulatedSplitHitThatIsAlreadyHardFluxIsNotConvertedAgain() {
        ShipAPI firing = shipAt(0f, 0);
        when(firing.getMutableStats().getDynamic().getValue(BeamSplitListener.TARGETS_KEY, 0f)).thenReturn(1f);
        when(firing.getMutableStats().getDynamic().getValue(ShieldSkillEffect.BeamHardFluxListener.HARD_FLUX_PERCENT_KEY, 0f)).thenReturn(50f);
        ShipAPI primary = shipAt(100f, 1);
        ShipAPI splitTarget = shipAt(200f, 1);
        ships.addAll(List.of(firing, primary, splitTarget));
        FluxTrackerAPI flux = splitTargetFlux(splitTarget, 100f, 150f, new boolean[1]);

        new BeamSplitListener(firing).modifyDamageDealt(beamFrom(simulatedWeapon()), primary, tick(true), new Vector2f(100f, 0f), true);

        verify(flux, never()).setHardFlux(anyFloat());
    }
}
