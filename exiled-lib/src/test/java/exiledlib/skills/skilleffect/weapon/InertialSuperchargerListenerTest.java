package exiledlib.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.ModManagerAPI;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.CollisionGridAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.util.DynamicStatsAPI;
import exiledlib.compat.LostSectorCompat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InertialSuperchargerListenerTest {

    private final List<Object> gridShips = new ArrayList<>();

    private MockedStatic<Global> globalMock;

    private CombatEngineAPI combatEngine;

    @BeforeEach
    void setUp() {
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        combatEngine = engine;
        CollisionGridAPI shipGrid = mock(CollisionGridAPI.class);
        when(engine.getShipGrid()).thenReturn(shipGrid);
        when(shipGrid.getCheckIterator(any(), anyFloat(), anyFloat())).thenAnswer(invocation -> new ArrayList<>(gridShips).iterator());
        globalMock = Mockito.mockStatic(Global.class);
        globalMock.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        globalMock.close();
    }

    private static ShipAPI ship(String id, float x, Map<String, Float> dynamicValues) {
        ShipAPI ship = mock(ShipAPI.class);
        MutableShipStatsAPI stats = mock(MutableShipStatsAPI.class);
        DynamicStatsAPI dynamic = mock(DynamicStatsAPI.class);
        when(ship.getId()).thenReturn(id);
        when(ship.getMutableStats()).thenReturn(stats);
        when(stats.getDynamic()).thenReturn(dynamic);
        dynamicValues.forEach((key, value) -> when(dynamic.getValue(key, 0f)).thenReturn(value));
        when(ship.getLocation()).thenReturn(new Vector2f(x, 0f));
        when(ship.getVelocity()).thenReturn(new Vector2f());
        when(ship.isAlive()).thenReturn(true);
        return ship;
    }

    @Test
    void inertialDamageFollowsSpeedAndOnlyUpdatesWhenTheWholePercentChanges() {
        ShipAPI ship = ship("a", 0f, Map.of(InertialSuperchargerListener.DAMAGE_PERCENT_PER_SPEED_KEY, 0.08f));
        MutableShipStatsAPI stats = ship.getMutableStats();
        MutableStat ballistic = mock(MutableStat.class);
        MutableStat energy = mock(MutableStat.class);
        MutableStat beam = mock(MutableStat.class);
        when(stats.getBallisticWeaponDamageMult()).thenReturn(ballistic);
        when(stats.getEnergyWeaponDamageMult()).thenReturn(energy);
        when(stats.getBeamWeaponDamageMult()).thenReturn(beam);
        InertialSuperchargerListener listener = new InertialSuperchargerListener(ship);

        ship.getVelocity().set(150f, 0f);
        listener.advance(0.016f);
        ship.getVelocity().set(151f, 0f);
        listener.advance(0.016f);

        verify(ballistic, times(1)).modifyPercent("exiledSector_inertialSupercharger_a", 12f);
        verify(energy, times(1)).modifyPercent("exiledSector_inertialSupercharger_a", 12f);
        verify(beam, times(1)).modifyPercent("exiledSector_inertialSupercharger_a", -12f);

        ship.getVelocity().set(0f, 0f);
        listener.advance(0.016f);

        verify(ballistic).unmodify("exiledSector_inertialSupercharger_a");
        verify(beam).unmodify("exiledSector_inertialSupercharger_a");
    }

    private void enableLostSector(boolean enabled) {
        SettingsAPI settings = mock(SettingsAPI.class);
        ModManagerAPI mods = mock(ModManagerAPI.class);
        when(settings.getModManager()).thenReturn(mods);
        when(mods.isModEnabled("lost_sector")).thenReturn(enabled);
        globalMock.when(Global::getSettings).thenReturn(settings);
    }

    private static ShipAPI augmentedInertialShip() {
        ShipAPI ship = ship("a", 0f, Map.of(InertialSuperchargerListener.DAMAGE_PERCENT_PER_SPEED_KEY, 0.08f,
                InertialSuperchargerListener.AUGMENTED_PROJECTILE_SPEED_KEY, 100f));
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(variant.hasHullMod(LostSectorCompat.AUGMENTED_SYSTEMS_HULLMOD_ID)).thenReturn(true);
        when(ship.getVariant()).thenReturn(variant);
        MutableShipStatsAPI stats = ship.getMutableStats();
        when(stats.getBallisticWeaponDamageMult()).thenReturn(mock(MutableStat.class));
        when(stats.getEnergyWeaponDamageMult()).thenReturn(mock(MutableStat.class));
        when(stats.getBeamWeaponDamageMult()).thenReturn(mock(MutableStat.class));
        when(stats.getProjectileSpeedMult()).thenReturn(mock(MutableStat.class));
        return ship;
    }

    @Test
    void withLostSectorAugmentedHullsAlsoGainProjectileSpeedFromTheDamageBonus() {
        enableLostSector(true);
        ShipAPI ship = augmentedInertialShip();
        InertialSuperchargerListener listener = new InertialSuperchargerListener(ship);

        ship.getVelocity().set(150f, 0f);
        listener.advance(0.016f);
        ship.getVelocity().set(0f, 0f);
        listener.advance(0.016f);

        verify(ship.getMutableStats().getProjectileSpeedMult()).modifyPercent("exiledSector_inertialSupercharger_a", 12f);
        verify(ship.getMutableStats().getProjectileSpeedMult()).unmodify("exiledSector_inertialSupercharger_a");
    }

    @Test
    void withoutLostSectorTheAugmentedProjectileSpeedNeverApplies() {
        enableLostSector(false);
        ShipAPI ship = augmentedInertialShip();
        InertialSuperchargerListener listener = new InertialSuperchargerListener(ship);

        ship.getVelocity().set(150f, 0f);
        listener.advance(0.016f);

        verify(ship.getMutableStats().getProjectileSpeedMult(), never()).modifyPercent(anyString(), anyFloat());
    }
}
