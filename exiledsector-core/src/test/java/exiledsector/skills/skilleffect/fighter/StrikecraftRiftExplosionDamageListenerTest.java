package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MutableStat;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.loading.DamagingExplosionSpec;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StrikecraftRiftExplosionDamageListenerTest {

    private static final Color INVISIBLE = new Color(0, 0, 0, 0);

    private static DamagingProjectileAPI explosion(String soundSetId) {
        DamagingExplosionSpec spec = mock(DamagingExplosionSpec.class);
        when(spec.getSoundSetId()).thenReturn(soundSetId);
        when(spec.getExplosionColor()).thenReturn(INVISIBLE);
        DamagingProjectileAPI projectile = mock(DamagingProjectileAPI.class);
        when(projectile.getExplosionSpecIfExplosion()).thenReturn(spec);
        return projectile;
    }

    private static DamageAPI damage() {
        DamageAPI damage = mock(DamageAPI.class);
        when(damage.getModifier()).thenReturn(mock(MutableStat.class));
        return damage;
    }

    private static StrikecraftRiftExplosionDamageListener listener(float percent) {
        return new StrikecraftRiftExplosionDamageListener(FighterCarriers.with(StrikecraftRiftExplosionDamageListener.DAMAGE_PERCENT_KEY, percent));
    }

    @Test
    void theCarriersBonusRaisesTheStrikeCraftsRiftExplosions() {
        DamageAPI damage = damage();

        String modId = listener(10f).modifyDamageDealt(explosion("riftbeam_rift"), mock(ShipAPI.class), damage, new Vector2f(), false);

        assertEquals(StrikecraftRiftExplosionDamageListener.DAMAGE_MOD_ID, modId);
        verify(damage.getModifier()).modifyPercent(StrikecraftRiftExplosionDamageListener.DAMAGE_MOD_ID, 10f);
    }

    @Test
    void otherExplosionsAndAMissingBonusAreLeftAlone() {
        DamageAPI flak = damage();
        DamageAPI rift = damage();

        assertNull(listener(10f).modifyDamageDealt(explosion("explosion_flak"), mock(ShipAPI.class), flak, new Vector2f(), false));
        assertNull(listener(0f).modifyDamageDealt(explosion("riftbeam_rift"), mock(ShipAPI.class), rift, new Vector2f(), false));

        verify(flak.getModifier(), never()).modifyPercent(anyString(), anyFloat());
        verify(rift.getModifier(), never()).modifyPercent(anyString(), anyFloat());
    }
}
