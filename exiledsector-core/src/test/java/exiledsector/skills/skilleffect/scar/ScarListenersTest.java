package exiledsector.skills.skilleffect.scar;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamageType;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.CombatListenerManagerAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScarListenersTest {

    private MockedStatic<Global> global;

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        HullModSpecAPI dModSpec = mock(HullModSpecAPI.class);
        when(dModSpec.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        when(settings.getHullModSpec("degraded_engines")).thenReturn(dModSpec);
        when(settings.getHullModSpec("heavyarmor")).thenReturn(mock(HullModSpecAPI.class));
        CombatEngineAPI engine = mock(CombatEngineAPI.class);
        CombatListenerManagerAPI listeners = mock(CombatListenerManagerAPI.class);
        when(engine.getListenerManager()).thenReturn(listeners);
        when(listeners.getListeners(any())).thenReturn(List.of());
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getSettings).thenReturn(settings);
        global.when(Global::getCombatEngine).thenReturn(engine);
    }

    @AfterEach
    void tearDown() {
        global.close();
    }

    private static ShipAPI ship(String id, int owner, String... hullMods) {
        ShipAPI ship = mock(ShipAPI.class);
        ShipVariantAPI variant = mock(ShipVariantAPI.class);
        when(ship.getId()).thenReturn(id);
        when(ship.getOwner()).thenReturn(owner);
        when(ship.isAlive()).thenReturn(true);
        when(ship.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        when(ship.getVariant()).thenReturn(variant);
        when(variant.getHullMods()).thenReturn(List.of(hullMods));
        return ship;
    }

    private static ShipAPI owner(String key, float magnitude) {
        ShipAPI owner = ship("boss", 1);
        when(owner.getMutableStats().getDynamic().getValue(key, 0f)).thenReturn(magnitude);
        return owner;
    }

    private static DamageAPI damage(DamageType type) {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        when(damage.getType()).thenReturn(type);
        return damage;
    }

    private static MissileAPI mine() {
        MissileAPI mine = mock(MissileAPI.class);
        when(mine.isMine()).thenReturn(true);
        return mine;
    }

    private static DamagingProjectileAPI shot(String weaponId) {
        DamagingProjectileAPI shot = mock(DamagingProjectileAPI.class);
        WeaponAPI weapon = mock(WeaponAPI.class);
        when(weapon.getId()).thenReturn(weaponId);
        when(shot.getWeapon()).thenReturn(weapon);
        when(shot.getProjectileSpecId()).thenReturn(weaponId + "_shot");
        return shot;
    }

    @Test
    void mineHitsOnTheOwnerTakeLessDamage() {
        ShipAPI owner = owner(MineDamageTakenListener.REDUCTION_PERCENT_KEY, 25f);
        MineDamageTakenListener listener = new MineDamageTakenListener(owner);
        DamageAPI hit = damage(DamageType.HIGH_EXPLOSIVE);

        String modId = listener.modifyDamageTaken(mine(), owner, hit, new Vector2f(), false);

        assertEquals("exiledSector_scarMineDamageTaken_boss", modId);
        verify(hit.getModifier()).modifyMult(modId, 0.75f);
    }

    @Test
    void ordinaryShotsAndHitsOnOtherShipsKeepTheirDamage() {
        ShipAPI owner = owner(MineDamageTakenListener.REDUCTION_PERCENT_KEY, 25f);
        MineDamageTakenListener listener = new MineDamageTakenListener(owner);
        DamageAPI hit = damage(DamageType.HIGH_EXPLOSIVE);

        assertNull(listener.modifyDamageTaken(shot("heavymauler"), owner, hit, new Vector2f(), false));
        assertNull(listener.modifyDamageTaken(mine(), ship("other", 1), hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }

    @Test
    void collisionReductionIgnoresWeaponHits() {
        ShipAPI owner = owner(CollisionDamageTakenListener.REDUCTION_PERCENT_KEY, 50f);
        CollisionDamageTakenListener listener = new CollisionDamageTakenListener(owner);
        DamageAPI hit = damage(DamageType.KINETIC);

        assertNull(listener.modifyDamageTaken(shot("heavymauler"), owner, hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyMult(anyString(), anyFloat());
    }

    @Test
    void thePlayerFlagshipTakesBonusDamageButOtherShipsDoNot() {
        PlayerFlagshipDamageListener listener = new PlayerFlagshipDamageListener(owner(PlayerFlagshipDamageListener.DAMAGE_PERCENT_KEY, 20f));
        ShipAPI flagship = ship("flagship", 0);
        PersonAPI player = mock(PersonAPI.class);
        when(player.isPlayer()).thenReturn(true);
        when(flagship.getCaptain()).thenReturn(player);
        ShipAPI escort = ship("escort", 0);
        when(escort.getCaptain()).thenReturn(mock(PersonAPI.class));
        DamageAPI flagshipHit = damage(DamageType.KINETIC);
        DamageAPI escortHit = damage(DamageType.KINETIC);

        String modId = listener.modifyDamageDealt(null, flagship, flagshipHit, new Vector2f(), false);

        assertEquals("exiledSector_scarPlayerFlagshipDamage_boss", modId);
        verify(flagshipHit.getModifier()).modifyPercent(modId, 20f);
        assertNull(listener.modifyDamageDealt(null, escort, escortHit, new Vector2f(), false));
        verify(escortHit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }

    @Test
    void dModdedTargetsTakeBonusDamageAndAreCheckedOnce() {
        DModdedShipDamageListener listener = new DModdedShipDamageListener(owner(DModdedShipDamageListener.DAMAGE_PERCENT_KEY, 10f));
        ShipAPI scarred = ship("scarred", 0, "heavyarmor", "degraded_engines");
        ShipAPI pristine = ship("pristine", 0, "heavyarmor");
        DamageAPI firstHit = damage(DamageType.KINETIC);
        DamageAPI secondHit = damage(DamageType.KINETIC);

        String modId = listener.modifyDamageDealt(null, scarred, firstHit, new Vector2f(), false);
        listener.modifyDamageDealt(null, scarred, secondHit, new Vector2f(), true);

        assertEquals("exiledSector_scarDModdedShipDamage_boss", modId);
        verify(firstHit.getModifier()).modifyPercent(modId, 10f);
        verify(secondHit.getModifier()).modifyPercent(modId, 10f);
        verify(scarred.getVariant(), times(1)).getHullMods();
        assertNull(listener.modifyDamageDealt(null, pristine, damage(DamageType.KINETIC), new Vector2f(), false));
    }

    @Test
    void kineticAndEnergyWeaponHitsOnEnemiesTurnHighExplosive() {
        HighExplosiveConversionListener listener = new HighExplosiveConversionListener(
                owner(HighExplosiveConversionListener.CHANCE_PERCENT_KEY, 100f), new Random(1));
        DamageAPI kinetic = damage(DamageType.KINETIC);
        DamageAPI fragmentation = damage(DamageType.FRAGMENTATION);
        DamageAPI allied = damage(DamageType.ENERGY);

        assertNull(listener.modifyDamageDealt(shot("heavymauler"), ship("enemy", 0), kinetic, new Vector2f(), false));
        listener.modifyDamageDealt(shot("vulcan"), ship("enemy", 0), fragmentation, new Vector2f(), false);
        listener.modifyDamageDealt(shot("pulselaser"), ship("ally", 1), allied, new Vector2f(), false);

        verify(kinetic).setType(DamageType.HIGH_EXPLOSIVE);
        verify(fragmentation, never()).setType(any());
        verify(allied, never()).setType(any());
    }

    @Test
    void noChanceMeansNoConversion() {
        HighExplosiveConversionListener listener = new HighExplosiveConversionListener(
                owner(HighExplosiveConversionListener.CHANCE_PERCENT_KEY, 0f), new Random(1));
        DamageAPI kinetic = damage(DamageType.KINETIC);

        listener.modifyDamageDealt(shot("heavymauler"), ship("enemy", 0), kinetic, new Vector2f(), false);

        verify(kinetic, never()).setType(any());
    }
}
