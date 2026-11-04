package exiledsector.skills.skilleffect.weapon;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PristineShipDamageListenerTest {

    private MockedStatic<Global> global;

    @BeforeEach
    void setUp() {
        SettingsAPI settings = mock(SettingsAPI.class);
        HullModSpecAPI dModSpec = mock(HullModSpecAPI.class);
        when(dModSpec.hasTag(Tags.HULLMOD_DMOD)).thenReturn(true);
        when(settings.getHullModSpec("degraded_engines")).thenReturn(dModSpec);
        when(settings.getHullModSpec("heavyarmor")).thenReturn(mock(HullModSpecAPI.class));
        global = Mockito.mockStatic(Global.class);
        global.when(Global::getSettings).thenReturn(settings);
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
        when(ship.getMutableStats()).thenReturn(mock(MutableShipStatsAPI.class, RETURNS_DEEP_STUBS));
        when(ship.getVariant()).thenReturn(variant);
        when(variant.getHullMods()).thenReturn(List.of(hullMods));
        return ship;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    private static PristineShipDamageListener listener(float percent) {
        ShipAPI owner = ship("reaper", 0);
        when(owner.getMutableStats().getDynamic().getValue(PristineShipDamageListener.DAMAGE_PERCENT_KEY, 0f)).thenReturn(percent);
        return new PristineShipDamageListener(owner);
    }

    @Test
    void hitsOnEnemyShipsWithoutDModsDealTheBonusDamage() {
        PristineShipDamageListener listener = listener(8f);
        ShipAPI pristine = ship("pristine", 1, "heavyarmor");
        DamageAPI firstHit = damage();
        DamageAPI secondHit = damage();

        String modId = listener.modifyDamageDealt(null, pristine, firstHit, new Vector2f(), false);
        listener.modifyDamageDealt(null, pristine, secondHit, new Vector2f(), true);

        assertEquals("exiledSector_pristineShipDamage_reaper", modId);
        verify(firstHit.getModifier()).modifyPercent(modId, 8f);
        verify(secondHit.getModifier()).modifyPercent(modId, 8f);
        verify(pristine.getVariant(), times(1)).getHullMods();
    }

    @Test
    void scarredShipsStrikecraftAndAlliesGetNoBonus() {
        PristineShipDamageListener listener = listener(8f);
        ShipAPI fighter = ship("fighter", 1);
        when(fighter.isFighter()).thenReturn(true);
        DamageAPI hit = damage();

        assertNull(listener.modifyDamageDealt(null, ship("scarred", 1, "heavyarmor", "degraded_engines"), hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, fighter, hit, new Vector2f(), false));
        assertNull(listener.modifyDamageDealt(null, ship("ally", 0), hit, new Vector2f(), false));
        assertNull(listener(0f).modifyDamageDealt(null, ship("pristine", 1), hit, new Vector2f(), false));
        verify(hit.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
