package exiledlib.motes;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnHitEffectPlugin;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import org.lwjgl.util.vector.Vector2f;

public final class MoteImpactSound implements OnHitEffectPlugin {

    static final String IMPACT_SOUND = "mote_attractor_impact_normal";

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit,
                      ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {
        if (Global.getSoundPlayer() != null) {
            Global.getSoundPlayer().playSound(IMPACT_SOUND, 1f, 1f, point, new Vector2f());
        }
    }
}
