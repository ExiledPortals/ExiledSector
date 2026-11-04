package exiledsector.skills.skilleffect.fighter;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.DamageListener;
import org.lwjgl.util.vector.Vector2f;

final class AccretionListener implements DamageDealtModifier, DamageListener {

    static final String DAMAGE_PERCENT_PER_STACK_KEY = "exiledSector_accretionDamagePercentPerStack";
    static final String DAMAGE_MOD_ID = "exiledSector_accretionDamage";

    private final ShipAPI fighter;
    private final ShipAPI carrier;
    private final AccretionStacks stacks;
    private boolean lossCounted;

    AccretionListener(ShipAPI fighter, ShipAPI carrier) {
        this.fighter = fighter;
        this.carrier = carrier;
        this.stacks = AccretionStacks.of(carrier);
    }

    private static float combatTime() {
        CombatEngineAPI engine = Global.getCombatEngine();
        return engine == null ? 0f : engine.getTotalElapsedTime(false);
    }

    @Override
    public void reportDamageApplied(Object source, CombatEntityAPI target, ApplyDamageResultAPI result) {
        if (lossCounted || target != fighter || fighter.getHitpoints() > 0f) {
            return;
        }
        lossCounted = true;
        stacks.add(combatTime());
    }

    @Override
    public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
        float percentPerStack = FighterListenerEffect.carrierValue(carrier, DAMAGE_PERCENT_PER_STACK_KEY);
        if (percentPerStack <= 0f) {
            return null;
        }
        int activeStacks = stacks.activeAt(combatTime());
        if (activeStacks == 0) {
            return null;
        }
        damage.getModifier().modifyPercent(DAMAGE_MOD_ID, percentPerStack * activeStacks);
        return DAMAGE_MOD_ID;
    }
}
