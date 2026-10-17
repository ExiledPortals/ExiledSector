package exiledsector.skills.skilleffect;

import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.DamageTakenModifier;
import org.lwjgl.util.vector.Vector2f;

final class WingNumbersDamageListener implements DamageTakenModifier {

    static final String REDUCTION_MOD_ID = "exiledSector_wingNumbersDamageTaken";
    static final float MAX_REDUCTION_PERCENT = 80f;

    private final ShipAPI fighter;
    private final float reductionPercentPerFighter;

    private WingNumbersDamageListener(ShipAPI fighter, float reductionPercentPerFighter) {
        this.fighter = fighter;
        this.reductionPercentPerFighter = reductionPercentPerFighter;
    }

    static void attach(ShipAPI fighter, float reductionPercentPerFighter) {
        if (fighter != null && reductionPercentPerFighter > 0f && !fighter.hasListenerOfClass(WingNumbersDamageListener.class)) {
            fighter.addListener(new WingNumbersDamageListener(fighter, reductionPercentPerFighter));
        }
    }

    static float damageTakenMult(float reductionPercentPerFighter, int livingFighters) {
        float reductionPercent = Math.min(MAX_REDUCTION_PERCENT, Math.max(0f, reductionPercentPerFighter * livingFighters));
        return 1f - reductionPercent / 100f;
    }

    @Override
    public String modifyDamageTaken(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        float mult = damageTakenMult(reductionPercentPerFighter, livingFighters());
        if (mult >= 1f) {
            return null;
        }
        damage.getModifier().modifyMult(REDUCTION_MOD_ID, mult);
        return REDUCTION_MOD_ID;
    }

    private int livingFighters() {
        FighterWingAPI wing = fighter.getWing();
        if (wing == null) {
            return CombatQueries.isAliveNotHulk(fighter) ? 1 : 0;
        }
        int living = 0;
        for (ShipAPI member : wing.getWingMembers()) {
            if (CombatQueries.isAliveNotHulk(member)) {
                living++;
            }
        }
        return living;
    }
}
