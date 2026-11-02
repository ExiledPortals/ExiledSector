package exiledsector.charts.effect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import exiledsector.charts.generation.ChartGenContext;
import org.lwjgl.util.vector.Vector2f;

final class ChartSModDamageListener implements DamageDealtModifier {

    static final String MOD_ID = ChartPlayerModifiers.MOD_ID + "_smod_damage";

    private final float mult;

    private ChartSModDamageListener(float mult) {
        this.mult = mult;
    }

    static ChartSModDamageListener of(float percentPerSMod, ShipVariantAPI variant) {
        int sMods = variant == null ? 0 : variant.getSMods().size();
        if (percentPerSMod == 0f || sMods == 0) {
            return null;
        }
        return new ChartSModDamageListener(Math.max(0f, 1f + percentPerSMod * sMods / ChartGenContext.PERCENT));
    }

    float mult() {
        return mult;
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        if (damage == null) {
            return null;
        }
        damage.getModifier().modifyMult(MOD_ID, mult);
        return MOD_ID;
    }
}
